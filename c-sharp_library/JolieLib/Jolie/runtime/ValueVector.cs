using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;
using System.Threading.Tasks;

namespace Jolie.runtime
{
    public class ValueVector : ISerializable, IEnumerable<Value>
    {
        protected List<Value> values = new List<Value>();

        public ValueVector() { }

        public Value First()
        {
            return Get(0);
        }

        public IEnumerator<Value> iterator()
        {
            return values.GetEnumerator();
            //return values.iterator();
        }

        public Value Get(int i)
        {
            if (i >= values.Count)
            {
                for (int k = values.Count; k <= i; k++)
                    values.Add(new Value());
            }
            return values[i];
        }

        public int Size()
        {
            return values.Count;
        }

        public void Set(int i, Value value)
        {
            if (i >= values.Count)
            {
                for (int k = values.Count; k < i; k++)
                    values.Add(new Value());
                values.Add(value);
            }
            else
            {
                values.Insert(i, value);
            }
        }

        public Boolean IsEmpty()
        {
            return values.Count == 0;
        }

        public void add(Value value)
        {
            values.Add(value);
        }

        public IEnumerator<Value> GetEnumerator()
        {
            return values.GetEnumerator();
        }

        System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator()
        {
            throw new NotImplementedException();
        }

        public void GetObjectData(SerializationInfo info, StreamingContext context)
        {
            throw new NotImplementedException();
        }
    }

    //class ValueVectorLink : ValueVector, ICloneable
    //{
    //    private readonly VariablePath linkPath;


    //    public Value get(int i)
    //    {
    //        return getLinkedValueVector().Get(i);
    //    }

    //    public void set(int i, Value value)
    //    {
    //        getLinkedValueVector().Set(i, value);
    //    }

    //    public ValueVectorLink(VariablePath path)
    //    {
    //        linkPath = path;
    //    }

    //    public Boolean isLink()
    //    {
    //        return true;
    //    }

    //    private ValueVector getLinkedValueVector()
    //    {
    //        return linkPath.GetValueVector;
    //    }

    //    protected List<Value> values()
    //    {
    //        //return getLinkedValueVector().values;
    //    }

    //    public object Clone()
    //    {
    //        return new ValueVectorLink(linkPath);
    //    }
    //}
}
