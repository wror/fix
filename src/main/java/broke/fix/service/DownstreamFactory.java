package broke.fix.service;

import broke.fix.Order;
import broke.fix.Parental;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.SimpleOrderRepo;
import broke.fix.misc.Validator;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class DownstreamFactory<F> {
	private final static Logger log = LogManager.getLogger();
	private final SimpleOrderRepo<F, Order<F>> repo;
	private final IdGenerator idgen;
	private final Collection<Validator<F>> validators;

	@Inject
	public DownstreamFactory(IdGenerator idgen, SimpleOrderRepo<F, Order<F>> repo, Collection<Validator<F>> validators) {
		this.idgen = idgen;
		this.repo = repo;
		this.validators = validators;
	}

	public String sliceOrInvalidMessage(Parental parent, long orderQty, F fields) {
		Order<F> order = repo.acquire().init(idgen.getClOrdID(), orderQty, fields);
		for (Validator<F> validator : validators) {
			String message = validator.getInvalidMessage(orderQty, fields, order.view());
			if (message != null) {
				repo.release(order);
				return message;
			}
		}
		parent.addChild(order);
		repo.addOrder(order);
		return null;
	}
}
