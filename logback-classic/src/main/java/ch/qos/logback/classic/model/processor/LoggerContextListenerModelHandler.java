package ch.qos.logback.classic.model.processor;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.model.LoggerContextListenerModel;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.OptionHelper;

public class LoggerContextListenerModelHandler extends ModelHandlerBase {
	boolean inError = false;
	LoggerContextListener lcl;

	public LoggerContextListenerModelHandler(Context context) {
		super(context);
	}

	@Override
	protected Class<LoggerContextListenerModel> getSupportedModelClass() {
		return LoggerContextListenerModel.class;
	}

	@Override
	public void handle(InterpretationContext intercon, Model model) throws ModelHandlerException {
		LoggerContextListenerModel LoggerContextListenerModel = (LoggerContextListenerModel) model;

		String className = LoggerContextListenerModel.getClassName();
		try {
			lcl = (LoggerContextListener) OptionHelper.instantiateByClassName(className, LoggerContextListener.class,
					context);

			if (lcl instanceof ContextAware) {
				((ContextAware) lcl).setContext(context);
			}

			intercon.pushObject(lcl);
			addInfo("Adding LoggerContextListener of type [" + className + "] to the object stack");

		} catch (Exception oops) {
			inError = true;
			addError("Could not create LoggerContextListener of type " + className + "].", oops);
		}
	}

	@Override
	public void postHandle(InterpretationContext interop, Model model) throws ModelHandlerException {
		if (inError) {
			return;
		}
		Object o = interop.peekObject();

		if (o != lcl) {
			addWarn("The object on the top the of the stack is not the LoggerContextListener pushed earlier.");
		} else {
			if (lcl instanceof LifeCycle) {
				((LifeCycle) lcl).start();
				addInfo("Starting LoggerContextListener");
			}
			((LoggerContext) context).addListener(lcl);
			interop.popObject();
		}
	}
}
