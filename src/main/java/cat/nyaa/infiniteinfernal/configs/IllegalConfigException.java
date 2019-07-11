package cat.nyaa.infiniteinfernal.configs;

public class IllegalConfigException extends RuntimeException {
    public IllegalConfigException(){
        super();
    }

    public IllegalConfigException(String s) {
        super(s);
    }
}
