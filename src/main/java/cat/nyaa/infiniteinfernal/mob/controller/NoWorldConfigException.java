package cat.nyaa.infiniteinfernal.mob.controller;

public class NoWorldConfigException extends RuntimeException{
    public NoWorldConfigException(){}

    public NoWorldConfigException(String message){
        super(message);
    }
}
