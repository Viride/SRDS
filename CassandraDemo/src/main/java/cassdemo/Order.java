package cassdemo;

public class Order {
    public String hall;
    public int clientId;
    public int clientRequestId;
    public int numberOfSeats;
    public long time;

    public Order(String hall, int clientId, int clientRequestId, int numberOfSeats, long time){
        this.hall=hall;
        this.clientId=clientId;
        this.clientRequestId=clientRequestId;
        this.numberOfSeats=numberOfSeats;
        this.time=time;
    }

    public Order() {}

    public long GetTime(){
        return this.time;
    }
}
