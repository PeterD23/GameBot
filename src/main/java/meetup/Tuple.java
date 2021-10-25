package meetup;

public class Tuple<T1, T2, T3> {

	private T1 t1;
	private T2 t2;
	private T3 t3;
	
	public Tuple(T1 t1, T2 t2, T3 t3) {
		this.t1 = t1;
		this.t2 = t2;
		this.t3 = t3;
	}
	
	public T1 first() {
		return t1;
	}
	
	public T2 second() {
		return t2;
	}
	
	public T3 third() {
		return t3;
	}
}
