using Jolie.net;
using Jolie.runtime;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;

namespace TestServer
{
    class Program
    {
        private static SodepProtocol protocol = new SodepProtocol();
        static void Main(string[] args)
        {
            TcpListener server = new TcpListener(IPAddress.Parse("127.0.0.1"), 9998);
            server.Start();

            Console.WriteLine("Server started. Waiting for incomming connections!");

            //while(true)
            //{
                var client = server.AcceptTcpClient();
                Stream stream = client.GetStream();

                Console.WriteLine("Client connected");

                CommMessage message = protocol.Recv(stream, stream);

                if(message != null)
                {
                    string operationName = message.OperationName;
                    string path = message.ResourcePath;
                    string type = message.Value.GetValueType().ToString();
                    string s = message.Value.StrValue;

                    Console.WriteLine("Children count: " + message.Value.Children.Count);
                    if(message.Value.Children.Count > 0)
                    {
                        foreach(KeyValuePair<string, ValueVector> entry in message.Value.Children)
                        {
                            Console.WriteLine("Children key: " + entry.Key);
                            foreach(Value v in entry.Value)
                            {
                                Console.WriteLine("Children value: " + v.StrValue);
                            }
                        }
                    }

                    Console.WriteLine("Type: " + type);
                    Console.WriteLine("Operation name: " + operationName);
                    Console.WriteLine("Resource path: " + path);
                    Console.WriteLine("Message: " + s);

                    Console.ReadKey();
                }

            //}
        }
    }
}
