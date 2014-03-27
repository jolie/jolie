using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Jolie.runtime
{
    public class ByteArray
    {
        private readonly byte[] buffer;

        public ByteArray(byte[] buffer)
        {
            this.buffer = buffer;
        }

        public int Size()
        {
            return buffer.Length;
        }

        public byte[] GetBytes()
        {
            return buffer;
        }

        public Boolean Equals(ByteArray other)
        {
            return Array.Equals(buffer, other.buffer);
        }

        public override String ToString()
        {
            char[] chars = new char[buffer.Length / 2];  // 2 bytes for each char
            for (int i = 0; i < chars.Length; i++)
            {
                for (int j = 0; j < 2; j++)
                {
                    int shift = (1 - j) * 8;
                    chars[i] |= (char)((0x000000FF << shift) & (((int)buffer[i * 2 + j]) << shift)); // added cast to char
                }
            }
            return new String(chars);
        }
    }
}
