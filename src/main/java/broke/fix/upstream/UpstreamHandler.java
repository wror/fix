package broke.fix.upstream;

import static broke.fix.misc.FixException.ordRejReason;
import static broke.fix.misc.FixException.cxlRejReason;

import java.util.Collection;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.Order;
import broke.fix.dto.CxlRejResponseTo;
import broke.fix.dto.ExecType;
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
	private final OrderListener<CompositeOrder<F>, F> publisher;
	private final Collection<Validator<F>> validators;
	public final IncomingContext incoming;
	public final UpstreamRepository<F> repo;

	@Inject
	public UpstreamHandler(IncomingContext incoming, OrderListener<CompositeOrder<F>, F> publisher, UpstreamRepository<F> repo, Collection<Validator<F>> validators) {
		this.incoming = incoming;
		this.publisher = publisher;
		this.repo = repo;
		this.validators = validators;
	}

	public void handleNewRequest(long transactTime, CharSequence instrumentID, CharSequence clOrdID, F fields) {
		CompositeOrder<F> order = new CompositeOrder<F>(incoming, fields, repo, publisher);
		run(()->{
			require(clOrdID, "clOrdID");
			require(instrumentID, "instrumentID");
			require(transactTime, "transactTime");
			require(fields.getOrderQty(), "orderQty");
			require(fields.getOrdType(), "ordType");
			require(fields.getSide(), "side");
			repo.checkForDuplicates(clOrdID);
			validate(order, order.getFields());
			repo.add(new NewRequest(order, clOrdID));
		}, clOrdID, transactTime, e->{publisher.onOtherExecutionReport(order, ExecType.Rejected, ordRejReason(e), e.getMessage());});
	}

	public void handleReplaceRequest(long transactTime, CharSequence instrumentID, CharSequence clOrdID, CharSequence origClOrdID, F fields) {
		CompositeOrder<F> order = repo.get(origClOrdID);
		run(()->{
			require(origClOrdID, "origClOrdID");
			require(clOrdID, "clOrdID");
			require(instrumentID, "instrumentID");
			require(transactTime, "transactTime");
			require(fields.getOrderQty(), "orderQty");
			require(fields.getOrdType(), "ordType");
			require(fields.getSide(), "side");
			sideMustMatch(order, fields);
			repo.checkForDuplicates(clOrdID);
			validate(order, fields);
			new ReplaceRequest<F>(order, fields, origClOrdID, clOrdID);
		}, clOrdID, transactTime, rejectHandler(order, CxlRejResponseTo.Replace, clOrdID));
	}

	private void sideMustMatch(CompositeOrder<F> order, F fields) {
		if (order.getFields().getSide() != fields.getSide()) {
			String message = "Attempt to amend side, from "+order.getFields().getSide()+" to "+fields.getSide();
			log.warn(message);
			throw new RuntimeException(message);
		}
	}

	public void handleCancelRequest(long transactTime, CharSequence instrumentID, CharSequence clOrdID, CharSequence origClOrdID) {
		CompositeOrder<F> order = repo.get(origClOrdID);
		run(()->{
			require(origClOrdID, "origClOrdID");
			require(clOrdID, "clOrdID");
			require(instrumentID, "instrumentID");
			require(transactTime, "transactTime");
			repo.checkForDuplicates(clOrdID);
			new CancelRequest(order, origClOrdID, clOrdID);
		}, clOrdID, transactTime, rejectHandler(order, CxlRejResponseTo.Cancel, clOrdID));
	}

	interface Runnable {
		void run() throws Exception;
	}

	private void run(Runnable r, CharSequence clOrdID, long transactTime, Consumer<Exception> c) {
		try {
			incoming.transactTime = transactTime;
			r.run();
		} catch (Exception e) {
			repo.addJustForDuplicateChecking(clOrdID);
			incoming.responseText = e.getMessage();
			log.warn("Rejected because of exception: {}", incoming.responseText);
			c.accept(e);
		}
	}

	private Consumer<Exception> rejectHandler(CompositeOrder<F> order, CxlRejResponseTo responseTo, CharSequence clOrdID) {
		return (e)->{publisher.onCancelOrReplaceReject(order, clOrdID, responseTo, cxlRejReason(order, e));};
	}

	private void validate(Order<F> order, F fields) {
		for (Validator<F> validator : validators) {
			CharSequence message = validator.getInvalidMessage(fields, order);
			if (message != null) {
				log.warn("Rejected: {}", message);
				throw new RuntimeException(message.toString());
			}
		}
	}

	private void require(Object value, String name) {
		if (value == null) {
			String message = "Missing required field "+name;
			log.warn(message);
			throw new RuntimeException(message);
		}
	}
}
