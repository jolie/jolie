using Jolie.net;
using Jolie.runtime;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;

namespace TestClient
{
    class Program
    {
        private static SodepProtocol protocol = new SodepProtocol();
        private static CommMessage initMessage()
        {
            ValueVector vec = new ValueVector();
            vec.add(new Value("Testing child of vector"));
            Value value = new Value("This is a test!");
            //Value value = new Value(666);
            //Value value = new Value(5.5);
            value.Children.Add("test", vec);
            FaultException fault = new FaultException("SAMPLE NAME", value);
            CommMessage message = new CommMessage(500L, "lol", "/test", value, null);
            return message;
        }
        static void Main(string[] args)
        {
            Console.WriteLine("Client ready, press any key to send message...");
            Console.ReadKey();

            TcpClient client = new TcpClient("127.0.0.1", 9998);
            Stream stream = client.GetStream();
            CommMessage msg = initMessage();


            protocol.Send(stream, msg, stream);

            //stream.Flush();
            //stream.Close();

            Console.WriteLine("Message sent to server. Press any key to exit client.");
            Console.ReadKey();
        }
    }
}
