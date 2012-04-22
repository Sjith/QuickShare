/**
 * Copyright (c) 2003,2004, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.ksoap2.serialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.SoapFault12;
import org.ksoap2.serialization.marshals.MarshalDefault;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Stefan Haustein
 *
 *         This class extends the SoapEnvelope with Soap Serialization functionality.
 */
public class SoapSerializationEnvelope extends SoapEnvelope {
    private static final String ANY_TYPE_LABEL = "anyType";
    private static final String ARRAY_MAPPING_NAME = "Array";
    private static final String NULL_LABEL = "null";
    private static final String NIL_LABEL = "nil";
    private static final String HREF_LABEL = "href";
    private static final String ID_LABEL = "id";
    private static final String ROOT_LABEL = "root";
    private static final String TYPE_LABEL = "type";
    private static final String ITEM_LABEL = "item";
    private static final String ARRAY_TYPE_LABEL = "arrayType";
    private static final Marshal DEFAULT_MARSHAL = new MarshalDefault();

    /**
     * Public properties that will be passed to getPropertyInfo method.
     * <p>
     * This field is not used int this class.
     */
    public Hashtable properties = new Hashtable();

    private final Map<String, Object> idMap = new HashMap<String, Object>();
    private List<Object> multiRef;

    /**
     * Set this variable to true if you don't want that type definitions for complex types/objects are automatically
     * generated (with type "anyType") in the XML-Request, if you don't call the Method addMapping. This is needed by
     * some Servers which have problems with these type-definitions.
     */
    public boolean implicitTypes;

    /**
     * Map from XML qualified names to Java classes
     */

    protected Map<QNameBase, Object> qNameToClass = new Hashtable();

    /**
     * Map from Java class names to XML type and namespace pairs
     */

    protected Map<String, QNameInfo> classToQName = new HashMap<String, QNameInfo>();

    public SoapSerializationEnvelope(int version) {
        super(version);
        addMapping(enc, ARRAY_MAPPING_NAME, PropertyInfo.VECTOR_CLASS);
        DEFAULT_MARSHAL.register(this);
    }

    /**
     * Set the bodyOut to be empty so that no un-needed xml is create. The null value for bodyOut will cause #writeBody
     * to skip writing anything redundant.
     *
     * @param emptyBody
     * @see "http://code.google.com/p/ksoap2-android/issues/detail?id=77"
     */
    public void setBodyOutEmpty(boolean emptyBody) {
        if (emptyBody) {
            bodyOut = null;
        }
    }

    @Override
    public void parseBody(XmlPullParser parser) throws IOException, XmlPullParserException {
        bodyIn = null;
        parser.nextTag();
        if (parser.getEventType() == XmlPullParser.START_TAG && parser.getNamespace().equals(env)
            && parser.getName().equals("Fault")) {
            SoapFault fault;
            if (this.version < SoapEnvelope.VER12) {
                fault = new SoapFault(this.version);
            } else {
                fault = new SoapFault12(this.version);
            }
            fault.parse(parser);
            bodyIn = fault;
        } else {
            while (parser.getEventType() == XmlPullParser.START_TAG) {
                String rootAttr = parser.getAttributeValue(enc, ROOT_LABEL);

                Object object = read(parser, null, -1, parser.getNamespace(), parser.getName(), PropertyInfo.OBJECT_TYPE);
                if ("1".equals(rootAttr) || bodyIn == null) {
                    bodyIn = object;
                }
                parser.nextTag();
            }
        }
    }

    /** Read a KvmSerializable.  */
    protected void readSerializable(XmlPullParser parser, KvmSerializable obj) throws IOException,
        XmlPullParserException {
        int propertyCount = obj.getPropertyCount();
        while (parser.nextTag() != XmlPullParser.END_TAG) {
            String parsedName = parser.getName();
            String parsedNamespace = parser.getNamespace();

            int foundIndex = -1;
            PropertyInfo propertyInfo = null;
            for (int i = 0; i < propertyCount; i++) {
                propertyInfo = obj.getPropertyInfo(i, properties);
                if ((propertyInfo.namespace == null || propertyInfo.namespace.equals(parsedNamespace))
                    && parsedName.equals(propertyInfo.name)) {
                    foundIndex = i;
                    break;
                }
            }

            if (foundIndex != -1) {
                Object value = read(parser, obj, foundIndex, null, null, propertyInfo);
                obj.setProperty(foundIndex, value);
            } else {
                throw new RuntimeException("Failed to find property " + parsedName + " in " + obj);
            }
        }
        parser.require(XmlPullParser.END_TAG, null, null);
    }

    private static int getIndex(String value, int start, int defaultIndex) {
        if (value == null) {
            return defaultIndex;
        }
        return value.length() - start < 3 ? defaultIndex :
            Integer.parseInt(value.substring(start + 1, value.length() - 1));
    }

    protected void readList(XmlPullParser parser, Vector list, PropertyInfo elementType) throws IOException, XmlPullParserException {
        String namespace = null;
        String name = null;
        int size = list.size();
        boolean dynamic = true;
        String type = parser.getAttributeValue(enc, ARRAY_TYPE_LABEL);
        if (type != null) {
            int cut0 = type.indexOf(':');
            int cut1 = type.indexOf("[", cut0);
            name = type.substring(cut0 + 1, cut1);
            String prefix = cut0 == -1 ? "" : type.substring(0, cut0);
            namespace = parser.getNamespace(prefix);
            size = getIndex(type, cut1, -1);
            if (size != -1) {
                list.setSize(size);
                dynamic = false;
            }
        }
        if (elementType == null) {
            elementType = PropertyInfo.OBJECT_TYPE;
        }
        parser.nextTag();
        int position = getIndex(parser.getAttributeValue(enc, "offset"), 0, 0);
        while (parser.getEventType() != XmlPullParser.END_TAG) {
            // handle position
            position = getIndex(parser.getAttributeValue(enc, "position"), 0, position);
            if (dynamic && position >= size) {
                size = position + 1;
                list.setSize(size);
            }
            // implicit handling of position exceeding specified size
            list.setElementAt(read(parser, list, position, namespace, name, elementType), position);
            position++;
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, null, null);
    }

    /**
     * Builds an object from the XML stream. This method is public for usage in conjunction with Marshal subclasses.
     * Precondition: On the start tag of the object or property, so href can be read.
     */

    public Object read(XmlPullParser parser, Object owner, int index, String namespace, String name,
        PropertyInfo expected) throws IOException, XmlPullParserException {
        System.out.println("Reading element: " + parser.getName());
        System.out.println("Owner: " + owner);
        System.out.println("Expected: " + expected);

        String elementName = parser.getName();
        String href = parser.getAttributeValue(null, HREF_LABEL);
        Object obj;
        if (href != null) {
            if (owner == null) {
                throw new RuntimeException("href at root level?!?");
            }
            href = href.substring(1);
            obj = idMap.get(href);
            if (obj == null || obj instanceof FwdRef) {
                FwdRef f = new FwdRef();
                f.next = (FwdRef) obj;
                f.obj = owner;
                f.index = index;
                idMap.put(href, f);
                obj = null;
            }
            parser.nextTag(); // start tag
            parser.require(XmlPullParser.END_TAG, null, elementName);
        } else {
            String nullAttr = parser.getAttributeValue(xsi, NIL_LABEL);
            String id = parser.getAttributeValue(null, ID_LABEL);
            if (nullAttr == null) {
                nullAttr = parser.getAttributeValue(xsi, NULL_LABEL);
            }
            if (nullAttr != null && SoapEnvelope.stringToBoolean(nullAttr)) {
                obj = null;
                parser.nextTag();
                parser.require(XmlPullParser.END_TAG, null, elementName);
            } else {
                String type = parser.getAttributeValue(xsi, TYPE_LABEL);
                if (type != null) {
                    int cut = type.indexOf(':');
                    name = type.substring(cut + 1);
                    String prefix = cut == -1 ? "" : type.substring(0, cut);
                    namespace = parser.getNamespace(prefix);
                } else if (name == null && namespace == null) {
                    if (parser.getAttributeValue(enc, ARRAY_TYPE_LABEL) != null) {
                        namespace = enc;
                        name = ARRAY_MAPPING_NAME;
                    } else {
                        QNameInfo names = getInfo(expected.type, null);
                        namespace = names.namespace;
                        name = names.type;
                    }
                }
                // be sure to set this flag if we don't know the types.
                if (type == null) {
                    implicitTypes = true;
                }
                obj = readInstance(parser, namespace, name, expected);
                if (obj == null) {
                    throw new RuntimeException("Failed to read object, name " + name + ", expected " + expected);
                }
            }
            // finally, care about the id....
            if (id != null) {
                Object hlp = idMap.get(id);
                if (hlp instanceof FwdRef) {
                    FwdRef f = (FwdRef) hlp;
                    do {
                        if (f.obj instanceof KvmSerializable) {
                            ((KvmSerializable) f.obj).setProperty(f.index, obj);
                        } else {
                            ((Vector) f.obj).setElementAt(obj, f.index);
                        }
                        f = f.next;
                    } while (f != null);
                } else if (hlp != null) {
                    throw new RuntimeException("double ID");
                }
                idMap.put(id, obj);
            }
        }

        parser.require(XmlPullParser.END_TAG, null, elementName);
        return obj;
    }

    /**
     * Returns a new object read from the given parser. If no mapping is found, null is returned. This method is used by
     * the SoapParser in order to convert the XML code to Java objects.
     */
    public Object readInstance(XmlPullParser parser, String namespace, String name, PropertyInfo expected)
        throws IOException, XmlPullParserException {
        Object obj = qNameToClass.get(new QNameBase(namespace, name));
        if (obj == null) {
            return null;
        }
        if (obj instanceof Marshal) {
            Marshal marshal = (Marshal) obj;
            return marshal.readInstance(parser, namespace, name, expected);
        } else if (obj == List.class) {
            obj = new ArrayList<Object>();
        } else {
            try {
                obj = ((Class) obj).newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create instance.", e);
            }
        }

        if (obj instanceof KvmSerializable) {
            readSerializable(parser, (KvmSerializable) obj);
        } else if (obj instanceof Vector) {
            readList(parser, (Vector) obj, expected.elementType);
        } else {
            throw new RuntimeException("no deserializer for " + obj.getClass());
        }
        return obj;
    }

    /**
     * Returns a string array containing the namespace, type, id and Marshal object for the given java object. This
     * method is used by the SoapWriter in order to map Java objects to the corresponding SOAP section five XML code.
     */
    public QNameInfo getInfo(Object type, Object instance) {
        if (type == null) {
            type = instance.getClass();
        }
        if (type != PropertyInfo.OBJECT_CLASS) {
            QNameInfo tmp = classToQName.get(((Class) type).getName());
            if (tmp != null) {
                return tmp;
            }
        }
        return new QNameInfo(xsd, ANY_TYPE_LABEL, null);
    }

    /**
     * Defines a direct mapping from a namespace and type to a java class (and vice versa), using the given marshal
     * mechanism
     */
    public void addMapping(String namespace, String name, Class clazz, Marshal marshal) {
        qNameToClass.put(new QNameBase(namespace, name), marshal == null ? (Object) clazz : marshal);
        classToQName.put(clazz.getName(), new QNameInfo(namespace, name, marshal));
    }

    /**
     * Defines a direct mapping from a namespace and type to a java class (and vice versa)
     */
    public void addMapping(String namespace, String name, Class clazz) {
        addMapping(namespace, name, clazz, null);
    }

    /**
     * Response from the soap call. Pulls the object from the wrapper object and returns it.
     *
     * @since 2.0.3
     * @return response from the soap call.
     * @throws SoapFault
     */
    public Object getResponse() throws SoapFault {
        if (bodyIn instanceof SoapFault) {
            throw (SoapFault) bodyIn;
        }
        KvmSerializable serailizableBody = (KvmSerializable) bodyIn;

        if (serailizableBody.getPropertyCount() == 0) {
            return null;
        } else if (serailizableBody.getPropertyCount() == 1) {
            return serailizableBody.getProperty(0);
        } else {
            List<Object> result = new ArrayList<Object>();
            for (int i = 0; i < serailizableBody.getPropertyCount(); i++) {
                result.add(serailizableBody.getProperty(i));
            }
            return result;
        }
    }

    /**
     * Serializes the request object to the given XmlSerliazer object
     *
     * @param writer XmlSerializer object to write the body into.
     */
    @Override
    public void writeBody(XmlSerializer writer) throws IOException {
        // allow an empty body without any tags in it
        // see http://code.google.com/p/ksoap2-android/issues/detail?id=77
        if (bodyOut != null) {
            multiRef = new ArrayList<Object>();
            multiRef.add(bodyOut);
            QNameInfo qName = getInfo(null, bodyOut);
            writer.startTag(qName.namespace, qName.type);
            writeElement(writer, bodyOut, null, qName.marshal);
            writer.endTag(qName.namespace, qName.type);
        }
    }

    /**
     * Writes the body of an KvmSerializable object. This method is public for access from Marshal subclasses.
     */
    public void writeObjectBody(XmlSerializer writer, KvmSerializable obj) throws IOException {
        for (int i = 0; i < obj.getPropertyCount(); i++) {
            // get the property
            Object propertyValue = obj.getProperty(i);
            // and importantly also get the property info which holds the type potentially!
            PropertyInfo propertyInfo = obj.getPropertyInfo(i, properties);

            if (propertyValue == null) {
                // skip null properties
            } else {
                // skip transient properties
                if ((propertyInfo.flags & PropertyInfo.TRANSIENT) == 0) {
                    writer.startTag(propertyInfo.namespace, propertyInfo.name);
                    writeProperty(writer, obj.getProperty(i), propertyInfo);
                    writer.endTag(propertyInfo.namespace, propertyInfo.name);
                }
            }
        }
    }

    protected void writeProperty(XmlSerializer writer, Object obj, PropertyInfo type) throws IOException {
        if (obj == null) {
            throw new RuntimeException("Property value should not be null.");
        }
        QNameInfo qName = getInfo(null, obj);
        if (type.multiRef) {
            int i = multiRef.indexOf(obj);
            if (i == -1) {
                i = multiRef.size();
                multiRef.add(obj);
            }
            writer.attribute(null, HREF_LABEL, "#o" + i);
        } else {
            if (!implicitTypes || obj.getClass() != type.type) {
                String prefix = writer.getPrefix(qName.namespace, true);
                writer.attribute(xsi, TYPE_LABEL, prefix + ":" + qName.type);
            }
            writeElement(writer, obj, type, qName.marshal);
        }
    }

    private void writeElement(XmlSerializer writer, Object element, PropertyInfo type, Object marshal)
        throws IOException {
        if (marshal != null) {
            ((Marshal) marshal).writeInstance(writer, element);
        } else if (element instanceof KvmSerializable) {
            writeObjectBody(writer, (KvmSerializable) element);
        } else if (element instanceof List) {
            writeListBody(writer, (List) element, type.elementType);
        } else {
            throw new RuntimeException("Cannot serialize: " + element);
        }
    }

    protected void writeListBody(XmlSerializer writer, List list, PropertyInfo elementType)
        throws IOException {
        String itemsTagName = ITEM_LABEL;
        String itemsNamespace = null;

        if (elementType == null) {
            elementType = PropertyInfo.OBJECT_TYPE;
        } else if (elementType instanceof PropertyInfo) {
            if (elementType.name != null) {
                itemsTagName = elementType.name;
                itemsNamespace = elementType.namespace;
            }
        }

        int cnt = list.size();
        QNameInfo arrType = getInfo(elementType.type, null);

        // This removes the arrayType attribute from the xml for arrays(required for most .Net services to work)
        if (!implicitTypes) {
            writer.attribute(enc, ARRAY_TYPE_LABEL, writer.getPrefix(arrType.namespace, false) + ":"
                + arrType.type + "[" + cnt + "]");
        }

        boolean skipped = false;
        for (int i = 0; i < cnt; i++) {
            if (list.get(i) == null) {
                skipped = true;
            } else {
                writer.startTag(itemsNamespace, itemsTagName);
                if (skipped) {
                    writer.attribute(enc, "position", "[" + i + "]");
                    skipped = false;
                }
                writeProperty(writer, list.get(i), elementType);
                writer.endTag(itemsNamespace, itemsTagName);
            }
        }
    }

    private static class FwdRef {

        private FwdRef next;
        private Object obj;
        private int index;
    }

    private static class QNameBase {
        protected final String namespace;
        protected final String type;

        protected QNameBase(String namespace, String type) {
            super();
            this.namespace = namespace;
            this.type = type;
        }

        @Override
        public boolean equals(Object otherObject) {
            if (!(otherObject instanceof QNameBase)) {
                return false;
            }
            QNameBase other = (QNameBase) otherObject;
            return type.equals(other.type) &&
                (namespace == null ? other.namespace == null : namespace.equals(other.namespace));
        }

        @Override
        public int hashCode() {
            return (type == null ? 0 : type.hashCode()) ^
                (namespace == null ? 0 : namespace.hashCode());
        }
    }

    private static class QNameInfo extends QNameBase {

        private final Marshal marshal;

        private QNameInfo(String namespace, String type, Marshal marshal) {
            super(namespace, type);
            this.marshal = marshal;
        }
    }
}