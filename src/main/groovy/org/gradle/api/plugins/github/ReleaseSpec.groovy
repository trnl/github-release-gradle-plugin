package org.gradle.api.plugins.github

class ReleaseSpec {

    private Object aTag
    private Object aName

    Object getTag() {
        aTag = call(aTag)
        return aTag
    }

    Object getName() {
        aName = call(aName)
        return aName
    }

    void setTag(final Object tag) {
        aTag = tag
    }

    void setName(final Object name) {
        aName = name
    }

    private static Object call(def c) {
        c instanceof Closure ? c.call() : c
    }
}
