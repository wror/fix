package broke.fix.upstream;

import static broke.fix.misc.FixException.ordRejReason;
import static broke.fix.misc.FixException.cxlRejReason;

import java.util.Collection;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.Order;
import broke.fix.Request;
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
	private final IncomingContext incoming;
	private final OrderListener<CompositeOrder<F>, F> publisher;
	private final UpstreamRepository<F> repo;
	private final Collection<Validator<F>> validators;

	@Inject
	public UpstreamHandler(IncomingContext incoming, OrderListener<CompositeOrder<F>, F> publisher, UpstreamRepository<F> repo, Collection<Validator<F>> validators) {
		this.incoming = incoming;
		this.publisher = publisher;
		this.repo = repo;
		this.validators = validators;
	}

	public void handleNewRequest(CharSequence clOrdID, F fields, long transactTime) {
		CompositeOrder<F> order = new CompositeOrder<F>(incoming, fields, repo, publisher);
		//TODO required fields: origclordid, clordid, transacttime, orderqty, ordtype, side, one of symbol or securityid and securityidsource
		run(()->{
			repo.checkForDuplicates(clOrdID);
			validate(order, order.getFields());
			repo.add(new NewRequest(order, clOrdID));
		}, clOrdID, transactTime, e->{publisher.onOtherExecutionReport(order, ExecType.Rejected, ordRejReason(e), e.getMessage());});
	}

	public void handleReplaceRequest(CharSequence clOrdID, CharSequence origClOrdID, long transactTime, F fields) {
		//TODO same required fields as on the New, plus origClOrdID
		//TODO fields must match: symbol, securityid, securityidsource, 
		CompositeOrder<F> order = repo.get(origClOrdID);
		run(()->{
			repo.checkForDuplicates(clOrdID);
			validate(order, fields);
			new ReplaceRequest<F>(order, fields, origClOrdID, clOrdID);
			//TODO is this too weird? it's the same as for downstream, and impossible to get wrong
		}, clOrdID, transactTime, rejectHandler(order, clOrdID));
	}

	public void handleCancelRequest(CharSequence clOrdID, CharSequence origClOrdID, long transactTime) {
		//TODO required fields: origclordid, clordid, transacttime, side, one of symbol or securityid and securityidsource
		CompositeOrder<F> order = repo.get(origClOrdID);
		run(()->{
			repo.checkForDuplicates(clOrdID);
			new CancelRequest(order, origClOrdID, clOrdID);
		}, clOrdID, transactTime, rejectHandler(order, clOrdID));
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

	private Consumer<Exception> rejectHandler(CompositeOrder<F> order, CharSequence clOrdID) {
		return (e)->{publisher.onCancelOrReplaceReject(order, clOrdID, cxlRejReason(order, e));};
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
}
