package net.melbourne.events;

import net.melbourne.Melbourne;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventHandler
{

    private final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private final List<Invoker.MethodInvoker> subscribedMethodInvokers = new CopyOnWriteArrayList<>();

    public EventHandler()
    {
    }

    public void subscribe(Object target)
    {
        try
        {
            SubscribableTarget subscribableTarget = new SubscribableTarget(target);
            for (Method method : subscribableTarget.getDeclaredMethods()) {

                if (method.isAnnotationPresent(SubscribeEvent.class) && method.getParameterCount() == 1)
                {
                    Parameter parameter = method.getParameters()[0];

                    if (Event.class.isAssignableFrom(parameter.getType()))
                    {
                        MethodType genericVoid = MethodType.methodType(void.class, Event.class);
                        MethodType strictVoid = MethodType.methodType(void.class, parameter.getType());
                        MethodType invokerType = subscribableTarget.retrieveInvoker();

                        MethodHandle handle = subscribableTarget.retrieveHandle(lookup, method);

                        CallSite callSite = LambdaMetafactory.metafactory(
                                lookup,
                                Invoker.class.getDeclaredMethods()[0].getName(),
                                invokerType,
                                genericVoid,
                                handle,
                                strictVoid
                        );

                        MethodHandle targetHandle = callSite.getTarget();

                        Invoker invoker = subscribableTarget.generateInvoker(targetHandle, target);
                        Invoker.MethodInvoker methodInvoker = new Invoker.MethodInvoker(method, invoker);

                        insertOrdinally(methodInvoker);
                    }
                }
            }
        }
        catch (Throwable t)
        {
            Melbourne.getLogger().error("Failed to register {}: ", target, t);
        }
    }

    public void unsubscribe(Object target) {
        SubscribableTarget subscribableTarget = new SubscribableTarget(target);
        
        for (Method method : subscribableTarget.getDeclaredMethods())
        {
            subscribedMethodInvokers.removeIf(methodInvoker -> methodInvoker.method().equals(method));
        }
    }

    public void post(Event event) {
        for (Invoker.MethodInvoker methodInvoker : subscribedMethodInvokers)
        {
            Method method = methodInvoker.method();
            
            if (event.getClass().isAssignableFrom(method.getParameters()[0].getType())) 
            {
                try 
                {
                    methodInvoker.invoker().invoke(event);
                } 
                catch (Throwable t) 
                {
                    Melbourne.getLogger().error("Failed while invoking an event: ", t);
                }
            }
        }
    }

    private void insertOrdinally(Invoker.MethodInvoker methodInvoker) {
        int index = 0;
        int priority = methodInvoker.getPriority();
        
        while (index < subscribedMethodInvokers.size())
        {
            int currPriority = subscribedMethodInvokers.get(index).getPriority();
            if (currPriority <= priority)
                break;
            
            index++;
        }
        
        subscribedMethodInvokers.add(index, methodInvoker);
    }

    public int getInvokers()
    {
        return subscribedMethodInvokers.size();
    }
}