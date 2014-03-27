using Jolie.runtime;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;
using System.Threading.Tasks;

namespace Jolie.net
{
    public class CommMessage : ISerializable
    {
        private static readonly long serialVersionUID = 1L;

        public static readonly String ROOT_RESOURCE_PATH = "/";
        private static long idCounter = 1L;

        //private static final AtomicLong idCounter = new AtomicLong( 1L );
        public static readonly long GENERIC_ID = 0L;
        public static readonly CommMessage UNDEFINED_MESSAGE = new CommMessage(GENERIC_ID, "", ROOT_RESOURCE_PATH, new Value(), null); //new CommMessage( GENERIC_ID, "", ROOT_RESOURCE_PATH, Value.Type.UNDEFINED, null );

        private readonly long id;
        private readonly String operationName;
        private readonly String resourcePath;
        private readonly Value value;
        private readonly FaultException fault;

        /**
         * Returns the resource path of this message.
         * @return the resource path of this message
         */
        public String ResourcePath
        {
            get
            {
                return resourcePath;
            }
        }

        /**
         * Returns <code>true</code> if this message has a generic identifier, <code>false</code> otherwise.
         *
         * A message with a generic identifier cannot be related to other messages.
         *
         * A message can have a generic identifier if it is meant to be used in a Notification.
         * Also, communication channels not supporting message identifiers could be generating
         * messages equipped with a generic identifier every time.
         * @return <code>true</code> if this message has a generic identifier, <code>false</code> otherwise
         */
        public Boolean HasGenericId
        {
            get
            {
                return id == GENERIC_ID;
            }
        }

        /**
         * Returns the identifier of this message.
         * @return the identifier of this message
         */
        public long Id
        {
            get
            {
                return id;
            }
        }

        private static long getNewMessageId()
        {
            //return idCounter.getAndIncrement();
            return idCounter++;
        }

        /**
         * Creates a request message.
         * @param operationName the name of the operation this request is meant for
         * @param resourcePath the resource path of this message
         * @param value the message data
         * @return a request message as per specified by the parameters
         */
        public static CommMessage createRequest(String operationName, String resourcePath, Value value)
        {
            return new CommMessage(getNewMessageId(), operationName, resourcePath, value, null); // Value supposed to be Value.deepCopy( value );
        }

        /**
         * Creates an empty (i.e. without data) response for the passed request.
         * @param request the request message that caused this response
         * @return an empty response for the passed request
         */
        public static CommMessage createEmptyResponse(CommMessage request)
        {
            return createResponse(request, new Value());
        }

        /**
         * Creates a response for the passed request.
         * @param request the request message that caused this response
         * @param value the data to equip the response with
         * @return a response for the passed request
         */
        public static CommMessage createResponse(CommMessage request, Value value)
        {
            //TODO support resourcePath
            return new CommMessage(request.id, request.operationName, "/", value, null); // Supposed to be Value.createDeepCopy( value )
        }

        /**
         * Creates a response message equipped with the passed fault.
         * @param request the request message that caused this response
         * @param fault the fault to equip the response with
         * @return a response message equipped with the specified fault
         */
        public static CommMessage createFaultResponse(CommMessage request, FaultException fault)
        {
            //TODO support resourcePath
            return new CommMessage(request.id, request.operationName, "/", new Value(), fault);
        }

        /**
         * Constructor
         * @param id the identifier for this message
         * @param operationName the operation name for this message
         * @param resourcePath the resource path for this message
         * @param value the message data to equip the message with
         * @param fault the fault to equip the message with
         */
        public CommMessage(long id, String operationName, String resourcePath, Value value, FaultException fault)
        {
            this.id = id;
            this.operationName = operationName;
            this.resourcePath = resourcePath;
            this.value = value;
            this.fault = fault;
        }

        /**
         * Constructor. The identifier of this message will be generic.
         * @param operationName the operation name for this message
         * @param resourcePath the resource path for this message
         * @param value the message data to equip the message with
         * @param fault the fault to equip the message with
         */
        /*private CommMessage( String operationName, String resourcePath, Value value, FaultException f )
        {
            this( GENERIC_ID, operationName, resourcePath, value, f );
        }*/

        /**
         * Constructor. The identifier of this message will be generic.
         * @param operationName the operation name of this message
         * @param resourcePath the resource path of this message
         */
        /*private CommMessage( String operationName, String resourcePath )
        {
            this( GENERIC_ID, operationName, resourcePath, Value.create(), null );
        }

        private CommMessage( long id, String operationName, String resourcePath, Value value )
        {
            this( id, operationName, resourcePath, value, null );
        }

        private CommMessage( long id, String operationName, String resourcePath, FaultException fault )
        {
            this( id, operationName, resourcePath, Value.create(), fault );
        }*/

        /**
         * Constructor. The identifier of this message will be generic.
         * @param operationName the operation name for this message
         * @param resourcePath the resource path for this message
         * @param value the message data to equip the message with
         */
        /*private CommMessage( String operationName, String resourcePath, Value value )
        {
            this( GENERIC_ID, operationName, resourcePath, value, null );
        }*/

        /**
         * Returns the value representing the data contained in this message.
         * @return the value representing the data contained in this message
         */
        public Value Value
        {
            get
            {
                return value;
            }
        }

        /**
         * The operation name of this message.
         * @return the operation name of this message
         */
        public String OperationName
        {
            get
            {
                return operationName;
            }
        }

        /**
         * Returns <code>true</code> if this message contains a fault, <code>false</code> otherwise.
         * @return <code>true</code> if this message contains a fault, <code>false</code> otherwise
         */
        public Boolean IsFault
        {
            get
            {
                return (fault != null);
            }
        }

        /**
         * Returns the fault contained in this message.
         *
         * If this message does not contain a fault, <code>null</code> is returned.
         * @return the fault contained in this message
         */
        public FaultException Fault
        {
            get
            {
                return fault;
            }
        }

        public void GetObjectData(SerializationInfo info, StreamingContext context)
        {
            throw new NotImplementedException();
        }
    }
}
