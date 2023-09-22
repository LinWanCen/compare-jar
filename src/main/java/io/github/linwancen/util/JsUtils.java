package io.github.linwancen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JsUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JsUtils.class);

    private JsUtils() {}

    public static Double calculate(String express) {
        Object o = eval(express.replace("_", ""));
        if (o == null) {
            return null;
        }
        String str = o.toString();
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            LOG.error("parseDouble NumberFormatException:\t{}", str, e);
            return null;
        }
    }

    public static Object eval(String js) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        try {
            return engine.eval(js);
        } catch (ScriptException e) {
            LOG.error("eval js ScriptException:\t{}", js, e);
            return null;
        }
    }
}
