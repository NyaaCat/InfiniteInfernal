package cat.nyaa.infiniteinfernal.ui;

import cat.nyaa.nyaacore.Message;

public abstract class BaseUi {
    public abstract Message buildMessage();

    public abstract IVar getVar(String name);


}
