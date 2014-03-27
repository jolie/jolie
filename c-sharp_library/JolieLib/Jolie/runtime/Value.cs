using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;
using System.Threading.Tasks;

namespace Jolie.runtime
{
    public class Value : ISerializable
    {
        //[Serializable]
        public enum Type
        {
            UNDEFINED, STRING, INT, DOUBLE, LONG, BOOLEAN, BYTEARRAY
        }

        private Dictionary<String, ValueVector> children = new Dictionary<String, ValueVector>();

        private String valueObject = null;
        private Type type = Type.UNDEFINED;

        public Boolean HasChildren { get { return children.Count > 0; } }

        public Dictionary<String, ValueVector> Children { get { return children == null ? new Dictionary<String, ValueVector>() : children; } }

        #region Identifiers

        public Type GetValueType()
        {
            if (IsString) return Type.STRING;
            else if (IsInt) return Type.INT;
            else if (IsDouble) return Type.DOUBLE;
            else if (IsLong) return Type.LONG;
            else if (IsBool) return Type.BOOLEAN;
            else if (IsByteArray) return Type.BYTEARRAY;
            else return Type.UNDEFINED;
        }

        public Boolean IsString { get { return type == Type.STRING; } }

        public Boolean IsInt { get { return type == Type.INT; } }

        public Boolean IsDouble { get { return type == Type.DOUBLE; } }

        public Boolean IsLong { get { return type == Type.LONG; } }

        public Boolean IsBool { get { return type == Type.BOOLEAN; } }

        public Boolean IsDefined { get { return type != Type.UNDEFINED; } }

        public Boolean IsByteArray { get { return type == Type.BYTEARRAY; } }

        #endregion

        #region Constructors

        public Value() { }

        public Value(ByteArray value)
        {
            SetValue(value);
        }

        public Value(String value)
        {
            SetValue(value);
        }

        public Value(Int32 value)
        {
            SetValue(value);
        }

        public Value(Double value)
        {
            SetValue(value);
        }

        // Added by Balint Maschio 
        public Value(Int64 value)
        {
            SetValue(value);
        }

        public Value(Boolean value)
        {
            SetValue(value);
        }

        #endregion

        #region Value methods

        public int IntValue
        {
            get
            {
                if (valueObject == null) return 0;
                return Int32.Parse(valueObject);
            }
        }

        public double DoubleValue
        {
            get
            {
                if (valueObject == null) return 0;
                return Double.Parse(valueObject);
            }
        }

        public String StrValue
        {
            get
            {
                if (valueObject == null) return "";
                return valueObject.ToString();
            }
        }

        // Added by Balint Maschio
        public long LongValue
        {
            get
            {
                if (valueObject == null) return 0;
                return long.Parse(valueObject);
            }
        }

        public Boolean BoolValue
        {
            get
            {
                if (valueObject == null) return false;
                return Boolean.Parse(valueObject);
            }
        }

        public ByteArray ByteArrayValue
        {
            get
            {
                ByteArray r = null;
                if (valueObject == null)
                {
                    byte[] resp = new byte[0];
                    return new ByteArray(resp);
                }
                else
                {
                    char[] chars = valueObject.ToCharArray();
                    byte[] byteArrayToReturn = new byte[chars.Length * 2];  //bytes per char = 2
                    for (int i = 0; i < chars.Length; i++)
                    {
                        for (int j = 0; j < 2; j++)
                            byteArrayToReturn[i * 2 + j] = (byte)(chars[i] >> (8 * (1 - j))); // OBS, original >>>
                    }
                    return new ByteArray(byteArrayToReturn);
                }
            }
        }

        #endregion

        public ValueVector GetChildren(String id)
        {
            ValueVector v = Children[id];
            if (v == null)
            {
                v = new ValueVector();
                children.Add(id, v);
            }
            return v;
        }

        public Boolean hasChildren(String id)
        {
            return children[id] != null;
        }

        public void deepCopy(Value otherValue)
        {
            valueObject = otherValue.valueObject;
            type = otherValue.type;
            ValueVector myVector;
            Value myValue;
            foreach (KeyValuePair<String, ValueVector> entry in otherValue.children)
            {
                myVector = new ValueVector();
                foreach (Value v in entry.Value)
                {
                    myValue = new Value();
                    myValue.deepCopy(v);
                    myVector.add(v);
                }
                children.Add(entry.Key, myVector);
            }
        }

        public Value GetNewChild(String childId)
        {
            ValueVector vec = GetChildren(childId);
            Value retVal = new Value();
            vec.add(retVal);
            return retVal;
        }

        public Value GetFirstChild(String id)
        {
            return GetChildren(id).First();
        }

        #region Set value methods

        public void SetValue(String obj)
        {
            valueObject = obj;
            type = Type.STRING;
        }

        public void SetValue(Int32 obj)
        {
            valueObject = obj.ToString();
            type = Type.INT;
        }

        public void SetValue(Double obj)
        {
            valueObject = obj.ToString();
            type = Type.DOUBLE;
        }

        public void SetValue(long obj)
        {
            valueObject = obj.ToString();
            type = Type.LONG;
        }

        public void SetValue(Boolean obj)
        {
            valueObject = obj.ToString();
            type = Type.BOOLEAN;
        }

        public void SetValue(ByteArray obj)
        {
            valueObject = obj.ToString();
            type = Type.BYTEARRAY;
        }

        #endregion

        public Boolean isUsedInCorrelation()
        {
            return false;
        }

        public void GetObjectData(SerializationInfo info, StreamingContext context)
        {
            throw new NotImplementedException();
        }
    }
}
