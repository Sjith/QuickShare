/*
 * Copyright (c) 2003,2004, Stefan Haustein, Oberhausen, Rhld., Germany Permission is hereby granted, free of charge, to
 * any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished
 * to do so, subject to the following conditions: The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF
 * ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. Contributor(s): John D. Beatty, F. Hunter,
 * Renaud Tognelli
 */

package org.ksoap2.serialization;

/**
 * This class is used to store information about each property an implementation of KvmSerializable exposes.
 */

public class PropertyInfo {
    public static final PropertyInfo OBJECT_TYPE = new PropertyInfo();
    public static final int TRANSIENT = 1;
    public static final int MULTI_REF = 2;
    public static final int REF_ONLY = 4;

    /**
     * Name of the property
     */
    public String name;

    /**
     * Namespace of this property
     */
    public String namespace;

    /**
     * Type of property, Transient, multi_ref, Ref_only *JHS* Note, not really used that effectively
     */
    public int flags;

    /**
     * The current value of this property.
     */
    protected Object value;

    /**
     * Type of the property/elements. Should usually be an instance of Class.
     */
    public Object type = Object.class;

    /**
     * if a property is multi-referenced, set this flag to true.
     */
    public boolean multiRef;

    /**
     * Element type for array properties, null if not array prop.
     */
    public PropertyInfo elementType;

    public PropertyInfo() {
    }

    public void clear() {
        type = Object.class;
        flags = 0;
        name = null;
        namespace = null;
    }

    /**
     * @return Returns the elementType.
     */
    public PropertyInfo getElementType() {
        return elementType;
    }

    /**
     * @param elementType The elementType to set.
     */
    public void setElementType(PropertyInfo elementType) {
        this.elementType = elementType;
    }

    /**
     * @return Returns the flags.
     */
    public int getFlags() {
        return flags;
    }

    /**
     * @param flags The flags to set.
     */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /**
     * @return Returns the multiRef.
     */
    public boolean isMultiRef() {
        return multiRef;
    }

    /**
     * @param multiRef The multiRef to set.
     */
    public void setMultiRef(boolean multiRef) {
        this.multiRef = multiRef;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the namespace.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace The namespace to set.
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return Returns the type.
     */
    public Object getType() {
        return type;
    }

    /**
     * @param type The type to set.
     */
    public void setType(Object type) {
        this.type = type;
    }

    /**
     * @return Returns the value.
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param value The value to set.
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Show the name and value.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append("(");
        sb.append(type);
        sb.append(")");
        sb.append(" = ");
        if (value != null) {
            sb.append(value);
        } else {
            sb.append("(not set)");
        }
        return sb.toString();
    }
}
