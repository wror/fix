package broke.fix.dto;

public enum ExecRestatementReason {
	GtCorporateAction(0), GtRenewal(1), VerbalChange(2), RepricingOfOrder(3), BrokerOption(4), PartialDeclineOforderQty(5), CancelOnTradingHalt(6), CancelOnSystemFailure(7), MarketOption(8), CanceledNotBest(9), WarehouseRecap(10), PegRefresh(11), Other(99);

	private final int code;

    private ExecRestatementReason(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
