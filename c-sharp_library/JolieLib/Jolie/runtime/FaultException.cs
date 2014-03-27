using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;
using System.Threading.Tasks;

namespace Jolie.runtime
{
    public class FaultException : Exception, ISerializable
    {
        private readonly string faultName;
        private readonly Value value;

        public FaultException() { }

        public FaultException(string faultName, Value value)
        {
            this.faultName = faultName;
            this.value = value;
        }

        public override string Message
        {
            get
            {
                return value.StrValue;
            }
        }

        public Value Value
        {
            get
            {
                return value;
            }
        }

        public string FaultName
        {
            get
            {
                return faultName;
            }
        }
    }
}
