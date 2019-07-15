package cat.nyaa.infiniteinfernal.controler;

public class NoWorldConfigException extends RuntimeException{
    public NoWorldConfigException(){}

    public NoWorldConfigException(String message){
        super(message);
    }
}
