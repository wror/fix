package broke.fix.misc;

public enum CxlRejReason {
	TooLateToCancel, UnknownOrder, BrokerExchangeption, AlreadyPendingCancelOrReplace, UnableToMassCancel, OrigOrdModTimeNotTransactTime, DuplicateClOrdID, Other,
	NotEnoughQuantity
}
