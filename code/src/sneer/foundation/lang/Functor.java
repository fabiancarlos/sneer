package sneer.foundation.lang;

public interface Functor<A, B> extends FunctorX<A, B, RuntimeException> {
	
	public static final Functor<Object, Object> IDENTITY = new Functor<Object, Object>() { @Override public Object evaluate(Object obj) {
		return obj;
	}};

	
	B evaluate(A value);
	
}
