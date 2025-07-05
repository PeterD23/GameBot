package trustsystem;

public interface Mergeable<T extends Mergeable<T>> {

	T merge(T mergeable);
	
}
