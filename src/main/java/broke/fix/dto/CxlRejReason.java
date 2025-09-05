package broke.fix.dto;

public enum CxlRejReason {
	TooLateToCancel, UnknownOrder, BrokerExchangeption, AlreadyPendingCancelOrReplace, UnableToMassCancel, OrigOrdModTimeNotTransactTime, DuplicateClOrdID, Other,
	NotEnoughQuantity
}
