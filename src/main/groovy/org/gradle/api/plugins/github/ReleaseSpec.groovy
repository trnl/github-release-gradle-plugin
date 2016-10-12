package org.gradle.api.plugins.github

class ReleaseSpec {

    private Object aTag
    private Object aName

    Object getTag() {
        return aTag
    }

    Object getName() {
        return aName
    }

    void setTag(final Object tag) {
        aTag = call(tag).toString()
    }

    void setName(final Object name) {
        aName = call(name).toString()
    }

    private static Object call(def c) {
        c instanceof Closure ? c.call() : c
    }
}
