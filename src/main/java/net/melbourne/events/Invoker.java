package net.melbourne.events;

import java.lang.reflect.Method;


@FunctionalInterface
public interface Invoker
{
	void invoke(Event event);

	record MethodInvoker(Method method, Invoker invoker)
	{
		public int getPriority()
		{
			return this.method.getAnnotation(SubscribeEvent.class).priority();
		}
	}
}