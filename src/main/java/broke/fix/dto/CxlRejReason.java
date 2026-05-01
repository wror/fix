package broke.fix.dto;

public enum CxlRejReason {
	TooLateToCancel(0), UnknownOrder(1), BrokerExchangeption(2), AlreadyPendingCancelOrReplace(3), UnableToMassCancel(4), OrigOrdModTimeNotTransactTime(5), DuplicateClOrdID(6), InvalidPriceIncrement(18), Other(99);

	private final int code;

    private CxlRejReason(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
