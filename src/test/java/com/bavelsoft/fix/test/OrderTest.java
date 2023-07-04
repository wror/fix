package com.bavelsoft.fix.test;
//leaves in a nonstandard package to not exploit any package private methods

import com.bavelsoft.fix.*;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientFields {
	double price;
}

class ExchangeFields {
	double price;
}

public class OrderTest {
	private RequestRepo clientRequestRepo;
	private RequestRepo exchangeRequestRepo;
	private OrderRepo clientOrderRepo;
	private ChildOrderRepo exchangeOrderRepo;
	private ChildOrderService orderService;
	private IdGenerator idgen;

	@BeforeEach
	public void setup() {
		idgen = new IdGenerator();
		clientRequestRepo = new RequestRepo(new HashMap());
		exchangeRequestRepo = new RequestRepo(new HashMap());
		clientOrderRepo = new OrderRepo(new HashMap(), new SimplePool(new ArrayList(), ()->new Order<ClientFields>(), 10), idgen, clientRequestRepo);
		exchangeOrderRepo = new ChildOrderRepo(
			new OrderRepo(new HashMap(), new SimplePool(new ArrayList(), ()->new ChildOrder<ExchangeFields>(), 10), idgen, exchangeRequestRepo),
			idgen, new ChildByParentRepo(new HashMap(), clientOrderRepo, ()->new ArrayList<>()));
		orderService = new ChildOrderService(idgen, exchangeRequestRepo, exchangeOrderRepo);
	}

	private void givenClientOrder(String clOrdID) {
		clientOrderRepo.requestNew(new ClientFields(), 100, "111_clOrdID_from_client");
	}
	
	private ChildOrder<ExchangeFields> givenExchangeOrder() {
		givenClientOrder("111_clOrdID_from_client");
		Order parent = clientRequestRepo.get("111_clOrdID_from_client").order;
		return exchangeOrderRepo.requestNew(new ExchangeFields(), 100, parent);
	}
	
	@Test
	public void testClientOrder() {
		Order order = clientOrderRepo.requestNew(new ClientFields(), 100, "111_clOrdID_from_client");

		assertEquals(OrdStatus.PendingNew, order.getOrdStatus());
	}

	@Test
	public void testClientAccept() {
		givenClientOrder("111_clOrdID_from_client");

		Request request = clientRequestRepo.get("111_clOrdID_from_client");
		request.accept();

		assertEquals(OrdStatus.New, request.order.getOrdStatus());
	}

	@Test
	public void testClientReject() {
		givenClientOrder("111_clOrdID_from_client");

		Request request = clientRequestRepo.get("111_clOrdID_from_client");
		request.reject();

		assertEquals(OrdStatus.Rejected, request.order.getOrdStatus());
	}

	@Test
	public void testClientReplaceRequest() {
		givenClientOrder("111_clOrdID_from_client");

		Order order = clientRequestRepo.get("111_clOrdID_from_client").order;
		clientRequestRepo.requestReplace(order, "222_clOrdID_from_client");

		assertEquals(OrdStatus.PendingReplace, order.getOrdStatus());
	}

	@Test
	public void testClientCancelRequest() {
		givenClientOrder("111_clOrdID_from_client");

		Order order = clientRequestRepo.get("111_clOrdID_from_client").order;
		clientRequestRepo.requestCancel(order, "333_clOrdID_from_client");

		assertEquals(OrdStatus.PendingCancel, order.getOrdStatus());
	}

	@Test
	public void testClientCancel() {
		givenClientOrder("111_clOrdID_from_client");

		Order order = clientRequestRepo.get("111_clOrdID_from_client").order;
		order.cancel();

		assertEquals(OrdStatus.Canceled, order.getOrdStatus());
	}

	@Test
	public void testExchangeOrder() {
		givenClientOrder("111_clOrdID_from_client");
		Order parent = clientRequestRepo.get("111_clOrdID_from_client").order;

		ExchangeFields fields = new ExchangeFields();
		ChildOrder<ExchangeFields> order = exchangeOrderRepo.requestNew(fields, 100, parent);

		assertEquals(OrdStatus.PendingNew, order.getOrdStatus());
		assertEquals(100, order.getWorkingQty());
	}

	@Test
	public void testExchangeCancelRequest() {
		ChildOrder<ExchangeFields> order = givenExchangeOrder();

		exchangeRequestRepo.requestCancel(order, idgen.getClOrdID());

		assertEquals(OrdStatus.PendingCancel, order.getOrdStatus());
	}

	@Test
	public void testExchangeReplaceRequest() {
		ChildOrder<ExchangeFields> order = givenExchangeOrder();

		RequestReplace<ExchangeFields> replaceRequest = exchangeRequestRepo.requestReplace(order, idgen.getClOrdID());
		ExchangeFields newFields = new ExchangeFields();
		newFields.price = 2;
		replaceRequest.pendingOrderQty = 120;
		replaceRequest.pendingFields = newFields;

		assertEquals(120, order.getWorkingQty());
	}

	@Test
	public void testExchangeAccept() {
		CharSequence clOrdID = givenExchangeOrder().getClOrdID();

		Order order = exchangeRequestRepo.get(clOrdID).order;
		boolean success = FixOrderUtil.exec(order, ExecType.New, clOrdID);

		assertEquals(true, success);
		assertEquals(OrdStatus.New, order.getOrdStatus());
	}

	@Test
	public void testExchangeReject() {
		CharSequence clOrdID = givenExchangeOrder().getClOrdID();

		Order order = exchangeRequestRepo.get(clOrdID).order;
		boolean success = FixOrderUtil.exec(order, ExecType.Rejected, clOrdID);

		assertEquals(true, success);
		assertEquals(OrdStatus.Rejected, order.getOrdStatus());
	}

	@Test
	public void testFullFill() {
		givenClientOrder("111_clOrdID_from_client");
		Order parent = clientRequestRepo.get("111_clOrdID_from_client").order;

		ChildOrder<ExchangeFields> order = exchangeOrderRepo.requestNew(new ExchangeFields(), 100, parent);
		order.fill(100, 1.0);
		exchangeOrderRepo.removeIfTerminal(order);
		//TODO test that both got evicted
	}
}

