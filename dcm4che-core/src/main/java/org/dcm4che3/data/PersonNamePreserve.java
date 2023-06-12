package org.dcm4che3.data;

public class PersonNamePreserve extends PersonNameBase {

    public PersonNamePreserve(String s, boolean lenient) {
        super(s, lenient);
    }

    @Override
    protected void handleEmptyComponent(int gindex, int cindex) {
        set(gindex, cindex, "");
    }

}
