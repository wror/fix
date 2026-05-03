package broke.fix.upstream;

import static broke.fix.upstream.ReasonExceptions.ordRejReason;
import static broke.fix.upstream.ReasonExceptions.cxlRejReason;

import java.util.Collection;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.Order;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecRestatementReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.CompositeOrder;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.OrderListener;
import broke.fix.misc.Validator;
import broke.fix.request.CancelRequest;
import broke.fix.request.NewRequest;
import broke.fix.request.ReplaceRequest;

public class UpstreamHandler<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private final IncomingContext incoming;
	private final OrderListener<CompositeOrder<F>> publisher;
	private final UpstreamRepository<F> repo;
	private final Collection<Validator<F>> validators;

	@Inject
	public UpstreamHandler(IncomingContext incoming, OrderListener<CompositeOrder<F>> publisher, UpstreamRepository<F> repo, Collection<Validator<F>> validators) {
		this.incoming = incoming;
		this.publisher = publisher;
		this.repo = repo;
		this.validators = validators;
	}

	public void handleNewRequest(CharSequence clOrdID, F fields, long transactTime) {
		CompositeOrder<F> order = new CompositeOrder<F>(incoming, fields, publisher, cleanupListener);
		//TODO required fields: origclordid, clordid, transacttime, orderqty, ordtype, side, one of symbol or securityid and securityidsource
		run(()->{
			validate(order);
			repo.add(new NewRequest<>(clOrdID, order));
			repo.add(order);
		}, clOrdID, transactTime, e->{publisher.onOtherExecutionReport(order, ExecType.Rejected, ordRejReason(e), null);});
	}

	public void handleReplaceRequest(CharSequence clOrdID, CharSequence origClOrdID, long transactTime, F fields) {
		CompositeOrder<F> order = repo.get(origClOrdID);
		//TODO same required fields as on the New, plus origClOrdID
		//TODO fields must match: symbol, securityid, securityidsource, 
		run(()->{
			validate(order);
			repo.add(new ReplaceRequest<>(clOrdID, order, fields));
		}, clOrdID, transactTime, handler(order, clOrdID));
	}

	public void handleCancelRequest(CharSequence clOrdID, CharSequence origClOrdID, long transactTime) {
		//TODO required fields: origclordid, clordid, transacttime, side, one of symbol or securityid and securityidsource
		CompositeOrder<F> order = repo.get(origClOrdID);
		run(()->{
			repo.add(new CancelRequest<>(clOrdID, order));
		}, clOrdID, transactTime, handler(order, clOrdID));
	}

	private void run(Runnable r, CharSequence clOrdID, long transactTime, Consumer<RuntimeException> c) {
		try {
			incoming.transactTime = transactTime;
			r.run();
		} catch (RuntimeException e) {
			repo.addJustForDuplicateChecking(clOrdID);
			incoming.responseText = e.getMessage();
			log.warn("Rejected because of exception: {}", incoming.responseText);
			c.accept(e);
		}
	}

	private Consumer<RuntimeException> handler(CompositeOrder<F> order, CharSequence clOrdID) {
		return (e)->{publisher.onCancelOrReplaceReject(order, clOrdID, order == null ? CxlRejReason.UnknownOrder : cxlRejReason(e));};
	}

	private void validate(Order<F> order) {
		for (Validator<F> validator : validators) {
			CharSequence message = validator.getInvalidMessage(order.getFields(), order);
			if (message != null) {
				log.warn("Rejected: {}", message);
				throw new RuntimeException(message.toString());
			}
		}
	}

	private OrderListener<CompositeOrder<F>> cleanupListener = new OrderListener<CompositeOrder<F>>() {
		@Override
		public void onTrade(CompositeOrder<F> order, ExecType execType, long qty, double px) {
			if (!order.isWorking()) {
				repo.remove(order);
			}
		}

		@Override
		public void onOtherExecutionReport(CompositeOrder<F> order, ExecType execType, OrdRejReason ordRejReason, ExecRestatementReason execRestatementReason) {
			if (!order.isWorking()) {
				repo.remove(order);
			}
		}
	};

}
