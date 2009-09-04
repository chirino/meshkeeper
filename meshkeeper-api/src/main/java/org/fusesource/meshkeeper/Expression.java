package org.fusesource.meshkeeper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.lang.*;
import java.lang.Process;

import org.fusesource.meshkeeper.launcher.LaunchAgent;
import org.fusesource.meshkeeper.util.internal.ProcessSupport;

/**
 * @author chirino
 */
abstract public class Expression implements Serializable {

    public final String evaluate() {
        return evaluate(System.getProperties());
    }

    abstract public String evaluate(java.util.Properties context);

    public static PropertyExpression property(String name) {
        return new PropertyExpression(name, null);
    }

    public static PropertyExpression property(String name, Expression defaultExpression) {
        return new PropertyExpression(name, defaultExpression);
    }

    public static StringExpression string(String value) {
        return new StringExpression(value);
    }

    public static FileExpression file(String value) {
        return new FileExpression(string(value));
    }

    public static FileExpression file(Expression value) {
        return new FileExpression(value);
    }

    public static PathExpression path(List<FileExpression> list) {
        return new PathExpression(list);
    }

    public static PathExpression path(FileExpression... value) {
        List<FileExpression> list = Arrays.asList(value);
        return path(list);
    }

    public static ExecExpression exec(List<Expression> value) {
        return new ExecExpression(value);
    }


    public static AppendExpression append(List<Expression> list) {
        return new AppendExpression(list);
    }

    public static AppendExpression append(Expression... value) {
        List<Expression> list = Arrays.asList(value);
        return append(list);
    }

    public static FileExpression resource(Resource resource) {
        return new FileExpression(append(property(LaunchAgent.LOCAL_REPO_PROP, string("local-repo")), file(File.separator + resource.getRepoPath())));
    }

    public static class StringExpression extends Expression {
        String value;

        public StringExpression(String value) {
            this.value = value;
        }

        public String evaluate(Properties p) {
            return value;
        }
    }

    public static class PropertyExpression extends Expression {
        String name;
        Expression defaultExpression;

        public PropertyExpression(String name, Expression defaultExpression) {
            this.name = name;
            this.defaultExpression = defaultExpression;
        }

        public String evaluate(Properties p) {
            String rc = System.getProperty(name);
            if (rc == null && defaultExpression != null) {
                rc = defaultExpression.evaluate(p);
            }
            return rc;
        }
    }

    public static class FileExpression extends Expression {
        Expression name;

        public FileExpression(Expression name) {
            this.name = name;
        }

        public String evaluate(Properties p) {
            String t = name.evaluate(p);
            if ('/' != File.separatorChar) {
                t.replace('/', File.separatorChar);
            } else {
                t.replace('\\', File.separatorChar);
            }
            return t;
        }
    }

    public static class PathExpression extends Expression {
        final ArrayList<FileExpression> files = new ArrayList<FileExpression>();

        public PathExpression(Collection<FileExpression> files) {
            this.files.addAll(files);
        }

        public String evaluate(Properties p) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (FileExpression file : files) {
                if (!first) {
                    sb.append(File.pathSeparatorChar);
                }
                first = false;
                sb.append(file.evaluate(p));
            }
            return sb.toString();
        }
    }


    public static class ExecExpression extends Expression {
        private final List<Expression> args;

        public ExecExpression(List<Expression> args) {
            this.args = args;
        }

        public String evaluate(Properties p) {
            String cmdline[] = new String[args.size()];
            for (int i = 0; i < cmdline.length; i++) {
                cmdline[i] = args.get(i).evaluate(p);
            }
            try {
                Process process = Runtime.getRuntime().exec(cmdline);
                if( process == null ) {
                    throw new RuntimeException("Could not execute "+cmdline[0]);
                }
                String rc = ProcessSupport.caputure(process);
                if( rc == null ) {
                    throw new RuntimeException("Command "+cmdline[0]+" failed");
                }
                return rc;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class AppendExpression extends Expression {
        final ArrayList<Expression> parts = new ArrayList<Expression>();

        public AppendExpression(Collection<Expression> parts) {
            this.parts.addAll(parts);
        }

        public String evaluate(Properties p) {
            StringBuilder sb = new StringBuilder();
            for (Expression expression : parts) {
                sb.append(expression.evaluate(p));
            }
            return sb.toString();
        }
    }

    public String toString() {
        return evaluate();
    }
}