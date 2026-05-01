package broke.fix.dto;

public enum OrdRejReason {
	BrokerOption(0), UnknownSymbol(1), ExchangeClosed(2), OrderExceedsLimit(3), TooLateToEnter(4), UnknownOrder(5), DuplicateClOrdID(6), DuplicateOfManual(7), StaleOrder(8), TradeAlongRequired(9), InvalidInvestorID(10), UnsupportedOrderCharacteristic(11), SurveillenceOption(12), IncorrectQuantity(13), IncorrectAllocatedQuantity(14), UnknownAccount(15), InvalidPriceIncrement(18), Other(99);

	private final int code;

    private OrdRejReason(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
