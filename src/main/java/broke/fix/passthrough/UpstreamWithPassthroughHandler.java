package broke.fix.passthrough;

import broke.fix.CompositeOrder;
import broke.fix.Order;
import broke.fix.dto.CxlRejReason;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.OrderListener;
import broke.fix.misc.Validator;
import broke.fix.upstream.UpstreamRepository;

import javax.inject.Inject;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

public class UpstreamWithPassthroughHandler {
	//<F extends FixFields> extends UpstreamHandler<F> {
	// private final PassthroughService passThroughService;
	// private final Consumer<CxlRejReason> rejector;

	// @Inject
	// public UpstreamWithPassthroughHandler(PassthroughService passThroughService, IncomingContext incoming, IdGenerator idgen, OrderListener<F> publisher, Set<CharSequence> clOrdIDs, UpstreamRepository<F> repo, Collection<Validator<F>> validators) {
	// 	super(incoming, idgen, publisher, repo, validators);
	// 	this.passThroughService = passThroughService;
	// 	this.rejector = reason->publisher.onCancelReject(null, reason);
	// }
	
	// @Override
	// protected void handleNewOrder(Order<F> order) {
	// 	super.handleNewOrder(order);
	// 	if (order.getFields().getExDestination() != null) {
	// 		passThroughService.slicePassthrough(order, order.getFields());
	// 	}
	// }

	// @Override
	// public void handleReplaceRequest(CharSequence orderID, CharSequence origClOrdID, CharSequence clOrdID, F fields) {
	// 	CompositeOrder<F> order = getOrder(orderID, clOrdID, rejector);
	// 	if (order != null && order.getFields().getExDestination() != null && fields.getExDestination() == null) {
	// 		passThroughService.requestCancelOfPassthrough(order);
	// 	}
	// 	super.handleReplaceRequest(orderID, origClOrdID, clOrdID, fields);
	// }
}
