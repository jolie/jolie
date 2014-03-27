using Jolie.runtime;
using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Jolie.net
{
    public class SodepProtocol// : ConcurrentCommProtocol
    {
        //private Encoding charSet = Encoding.UTF8;

        private static class DataTypeHeaderId
        {
            public const int NULL = 0;
            public const int STRING = 1;
            public const int INT = 2;
            public const int DOUBLE = 3;
            public const int BYTE_ARRAY = 4;
            public const int BOOL = 5;
            public const int LONG = 6;
        }

        public string Name()
        {
            return "sodep";
        }

        private string ReadString(BinaryReader reader)
        {
            try
            {
                int length = ReadInt(reader);
                if (length > 0)
                {
                    byte[] bb = new byte[length];
                    reader.Read(bb, 0, length);
                    return Encoding.Default.GetString(bb);
                }
                return "";
            }
            catch (Exception e) { Console.WriteLine(e.Message); return ""; }
        }

        private void WriteString(BinaryWriter writer, string s)
        {
            try
            {
                if (String.IsNullOrEmpty(s)) writer.Write(0);
                else
                {
                    MemoryStream mStream = new MemoryStream();
                    StreamWriter sWriter = new StreamWriter(mStream, Encoding.Default);
                    sWriter.Write(s);
                    sWriter.Flush();
                    //sWriter.Close();
                    byte[] bb = mStream.ToArray();
                    writer.Write(WriteInt(bb.Length));
                    //writer.Write(bb.Length);
                    writer.Write(bb);
                }
            }
            catch (Exception e) { Console.WriteLine(e.Message); }
        }

        private ByteArray ReadByteArray(BinaryReader reader)
        {
            try
            {
                //int size = reader.ReadInt32();
                int size = ReadInt(reader);
                ByteArray ret = null;
                if (size > 0)
                {
                    byte[] bytes = new byte[size];
                    reader.Read(bytes, 0, size);
                    ret = new ByteArray(bytes);
                }
                else
                {
                    ret = new ByteArray(new byte[0]);
                }
                return ret;
            }
            catch (IOException) { return new ByteArray(new byte[0]); }
        }

        private void WriteByteArray(BinaryWriter writer, ByteArray byteArray)
        {
            try
            {
                int size = byteArray.Size();
                //writer.Write(size);
                writer.Write(WriteInt(size));
                if (size > 0)
                {
                    writer.Write(byteArray.GetBytes());
                }
            }
            catch (IOException e) { }
        }

        private void WriteFault(BinaryWriter writer, FaultException fault)
        {
            try
            {
                WriteString(writer, fault.FaultName);
                WriteValue(writer, fault.Value);
            }
            catch (IOException e) { }
        }

        private void WriteValue(BinaryWriter writer, Value value)
        {
            try
            {
                if (value.IsString)
                {
                    writer.Write((byte)DataTypeHeaderId.STRING);
                    WriteString(writer, value.StrValue);
                }
                else if (value.IsInt)
                {
                    writer.Write((byte)DataTypeHeaderId.INT);
                    writer.Write(value.IntValue);
                }
                else if (value.IsDouble)
                {
                    writer.Write((byte)DataTypeHeaderId.DOUBLE);
                    writer.Write(value.DoubleValue);
                }
                else if (value.IsByteArray)
                {
                    writer.Write((byte)DataTypeHeaderId.BYTE_ARRAY);
                    WriteByteArray(writer, value.ByteArrayValue);
                }
                else if (value.IsBool)
                {
                    writer.Write((byte)DataTypeHeaderId.BOOL);
                    writer.Write(value.BoolValue);
                }
                else if (value.IsLong)
                {
                    writer.Write((byte)DataTypeHeaderId.LONG);
                    writer.Write(value.LongValue);
                }
                else
                {
                    writer.Write((byte)DataTypeHeaderId.NULL);
                }

                Dictionary<string, ValueVector> children = value.Children;
                LinkedList<KeyValuePair<string, ValueVector>> entries = new LinkedList<KeyValuePair<string, ValueVector>>(); // OBS!
                foreach (KeyValuePair<string, ValueVector> entry in children)
                {
                    entries.AddLast(entry);
                }

                //writer.Write(entries.Count);
                writer.Write(WriteInt(entries.Count));

                foreach (KeyValuePair<string, ValueVector> entry in entries)
                {
                    WriteString(writer, entry.Key);
                    //writer.Write(entry.Value.Size());
                    writer.Write(WriteInt(entry.Value.Size()));
                    foreach (Value v in entry.Value)
                    {
                        WriteValue(writer, v);
                    }
                }
            }
            catch (IOException e) { }
        }

        private void WriteMessage(BinaryWriter writer, CommMessage message)
        {
            try
            {
                writer.Write(WriteLong(message.Id));
                WriteString(writer, message.ResourcePath);
                WriteString(writer, message.OperationName);
                FaultException fault = message.Fault;
                if (fault == null) writer.Write(false);
                else
                {
                    writer.Write(true);
                    WriteFault(writer, fault);
                }
                WriteValue(writer, message.Value);
            }
            catch (IOException e) { }
        }

        private Value ReadValue(BinaryReader reader)
        {
            try
            {
                Value value = new Value();
                byte b = reader.ReadByte();
                //sbyte b = reader.ReadSByte();
                switch (b)
                {
                    case DataTypeHeaderId.STRING:
                        value = new Value(ReadString(reader));
                        break;
                    case DataTypeHeaderId.INT:
                        value = new Value(ReadInt(reader));
                        break;
                    case DataTypeHeaderId.LONG:
                        value = new Value(ReadLong(reader));
                        break;
                    case DataTypeHeaderId.DOUBLE:
                        value = new Value(reader.ReadDouble());
                        break;
                    case DataTypeHeaderId.BYTE_ARRAY:
                        value = new Value(ReadByteArray(reader));
                        break;
                    case DataTypeHeaderId.BOOL:
                        value = new Value(reader.ReadBoolean());
                        break;
                    case DataTypeHeaderId.NULL:
                        break;
                    default:
                        break;
                }

                Dictionary<string, ValueVector> children = value.Children;
                string s;
                int n, i, size, k;
                //n = reader.ReadInt32();
                n = ReadInt(reader);
                ValueVector vec;

                for (i = 0; i < n; i++)
                {
                    s = ReadString(reader);
                    vec = new ValueVector();
                    //size = reader.ReadInt32();
                    size = ReadInt(reader);
                    for (k = 0; k < size; k++)
                    {
                        vec.add(ReadValue(reader));
                    }
                    if (!children.ContainsKey(s)) children.Add(s, vec);
                }
                return value;
            }
            catch (Exception e) { Console.WriteLine(e.Message); return null; } // OBS!
        }

        private FaultException ReadFault(BinaryReader reader)
        {
            try
            {
                string faultName = ReadString(reader);
                Value value = ReadValue(reader);
                return new FaultException(faultName, value);
            }
            catch (IOException e) { return null; }
        }

        // Big- Little endian convert
        private long ReadLong(BinaryReader reader)
        {
            byte[] arr = reader.ReadBytes(8);
            Array.Reverse(arr);
            return BitConverter.ToInt64(arr, 0);
        }

        private byte[] WriteLong(long l)
        {
            byte[] arr = BitConverter.GetBytes(l);
            Array.Reverse(arr);
            return arr;
        }

        // Big- Little endian convert
        private int ReadInt(BinaryReader reader)
        {
            byte[] arr = reader.ReadBytes(4);
            Array.Reverse(arr);
            return BitConverter.ToInt32(arr, 0);
        }

        private byte[] WriteInt(int i)
        {
            byte[] arr = BitConverter.GetBytes(i);
            Array.Reverse(arr);
            return arr;
        }

        private CommMessage ReadMessage(BinaryReader reader)
        {
            try
            {
                long id = ReadLong(reader);

                string resourcePath = ReadString(reader);
                string operationName = ReadString(reader);

                FaultException fault = null;
                if (reader.ReadBoolean() == true)
                {
                    fault = ReadFault(reader);
                }

                Value value = ReadValue(reader);
                return new CommMessage(id, operationName, resourcePath, value, fault);
            }
            catch (IOException e) { return null; }
        }

        //public SodepProtocol(VariablePath configurationPath)
        //{
        //    super(configurationPath);
        //}

        public void Send(Stream oStream, CommMessage message, Stream iStream)
        {
            try
            {
                // channel.setToBeClosed( !checkBooleanParameter( "KeepAlive", true ) );

                //string charset = 

                Encoding charSet = Encoding.UTF8;
                GZipStream gzip = null;

                // Check for compression
                //gzip = new GZipStream(oStream, CompressionLevel.Fastest);
                //oStream = gzip;

                BinaryWriter oos = new BinaryWriter(oStream);
                WriteMessage(oos, message);
                if (gzip != null)
                    gzip.Close();
            }
            catch (IOException e) { }
        }

        public CommMessage Recv(Stream iStream, Stream oStream)
        {
            try
            {
                Encoding charSet = Encoding.UTF8;

                // Check for compression
                //iStream = new GZipStream(iStream, CompressionLevel.Fastest);

                BinaryReader ios = new BinaryReader(iStream);
                return ReadMessage(ios);
                
            }
            catch (IOException e) { return null; }
        }
    }
}
