package net.melbourne.events;

import lombok.Getter;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;


@Getter
public final class SubscribableTarget
{
	private final Class<?> targetClass;
	private final AccessType accessType;

	public SubscribableTarget(Object target)
	{
		targetClass =
				target instanceof Class<?> ? (Class<?>) target : target.getClass();
		accessType =
				target instanceof Class<?> ? AccessType.STATIC : AccessType.VIRTUAL;
	}

	public Method[] getDeclaredMethods()
	{
		return switch (accessType)
		{
			case STATIC -> Arrays.stream(targetClass.getDeclaredMethods()).filter(m -> (m.getModifiers() & Opcodes.ACC_STATIC) != 0)
					.toArray(Method[]::new);
			case VIRTUAL -> Arrays.stream(targetClass.getDeclaredMethods()).filter(m -> (m.getModifiers() & Opcodes.ACC_STATIC) == 0)
					.toArray(Method[]::new);
		};
	}

	public MethodType retrieveInvoker()
	{
		return switch (accessType)
		{
			case STATIC -> MethodType.methodType(Invoker.class);
			case VIRTUAL -> MethodType.methodType(Invoker.class, getTargetClass());
		};
	}

	public MethodHandle retrieveHandle(MethodHandles.Lookup lookup, Method method) throws NoSuchMethodException, IllegalAccessException
	{
		return switch (accessType)
		{
			case STATIC -> lookup.findStatic(targetClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameters()[0]
					.getType()));
			case VIRTUAL -> lookup.findVirtual(targetClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameters()[0]
					.getType()));
		};
	}

	public Invoker generateInvoker(MethodHandle targetHandle, Object instance) throws Throwable
	{
		return switch (accessType)
		{
			case STATIC -> (Invoker) targetHandle.invokeExact();
			case VIRTUAL -> (Invoker) targetHandle.invoke(instance);
		};
	}

	public enum AccessType
	{
		STATIC,
		VIRTUAL
	}
}