package broke.fix.misc;

public enum ExecInst {
	Suspend('S');

	//TODO character values for all the fix enums

	char c;

	ExecInst(char c) {
		this.c = c;
	}
}
