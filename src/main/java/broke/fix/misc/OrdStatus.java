package broke.fix.misc;

public enum OrdStatus {
	New('0'), PartiallyFilled('1'), Filled('2'), DoneForDay('3'), Canceled('4'), PendingCancel('6'), Rejected('8'), Suspended('9'), PendingNew('A'), PendingReplace('E');

	char c;

	OrdStatus(char c) {
		this.c = c;
	}
}
