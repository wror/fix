package broke.fix.upstream;

import static broke.fix.upstream.ReasonExceptions.ordRejReason;
import static broke.fix.RequestWithUpstreamClOrdID.with;
import static broke.fix.upstream.ReasonExceptions.cxlRejReason;

import java.util.Collection;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.Order;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.CompositeOrder;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.OrderListener;
import broke.fix.misc.Validator;

public class UpstreamHandler<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private final IncomingContext incoming;
	protected final OrderListener<CompositeOrder<F>> publisher;
	private final UpstreamRepository repo;
	private final Collection<Validator<F>> validators;

	@Inject
	public UpstreamHandler(IncomingContext incoming, OrderListener<CompositeOrder<F>> publisher, UpstreamRepository repo, Collection<Validator<F>> validators) {
		this.incoming = incoming;
		this.publisher = publisher;
		this.repo = repo;
		this.validators = validators;
	}

	public void handleNewRequest(CharSequence clOrdID, F fields, long transactTime) {
		CompositeOrder<F> order = new CompositeOrder<F>(incoming, fields, publisher);
		run(()->{
			validate(order);
			repo.add(with(clOrdID, order.requestNew()));
		}, clOrdID, transactTime, e->{publisher.onOtherExecutionReport(order, ExecType.Rejected, ordRejReason(e), null);});
	}

	public void handleReplaceRequest(CharSequence clOrdID, CharSequence origClOrdID, long transactTime, F fields) {
		CompositeOrder<F> order = (CompositeOrder<F>)repo.get(origClOrdID);
		run(()->{
			validate(order);
			repo.add(with(clOrdID, order.requestReplace(fields)));
		}, clOrdID, transactTime, handler(order, clOrdID));
	}

	public void handleCancelRequest(CharSequence clOrdID, CharSequence origClOrdID, long transactTime) {
		CompositeOrder<F> order = (CompositeOrder<F>)repo.get(origClOrdID);
		run(()->{
			repo.add(with(clOrdID, order.requestCancel()));
		}, clOrdID, transactTime, handler(order, clOrdID));
	}

	private void run(Runnable r, CharSequence clOrdID, long transactTime, Consumer<RuntimeException> c) {
		try {
			incoming.transactTime = transactTime;
			r.run();
		} catch (RuntimeException e) {
			repo.addForDuplicateChecking(clOrdID);
			incoming.responseText = e.getMessage();
			log.warn("Rejected because of exception: {}", incoming.responseText);
			c.accept(e);
		}
	}

	private Consumer<RuntimeException> handler(CompositeOrder<F> order, CharSequence clOrdID) {
		return (e)->{publisher.onCancelReject(order, clOrdID, order == null ? CxlRejReason.UnknownOrder : cxlRejReason(e));};
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
}
