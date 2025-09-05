package broke.fix.service;

import broke.fix.Order;
import broke.fix.OrderComposite;
import broke.fix.misc.FixFields;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.FixRepository;
import broke.fix.misc.Validator;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class DownstreamFactory<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private final FixRepository<F, Order<F>> repo;
	private final IdGenerator idgen;
	private final Collection<Validator<F>> validators;

	@Inject
	public DownstreamFactory(IdGenerator idgen, FixRepository<F, Order<F>> repo, Collection<Validator<F>> validators) {
		this.idgen = idgen;
		this.repo = repo;
		this.validators = validators;
	}

	public CharSequence sliceOrInvalidMessage(OrderComposite parent, F fields) {
		Order<F> order = repo.acquire().init(idgen.getClOrdID(), fields);
		for (Validator<F> validator : validators) {
			CharSequence message = validator.getInvalidMessage(fields, order.view());
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
