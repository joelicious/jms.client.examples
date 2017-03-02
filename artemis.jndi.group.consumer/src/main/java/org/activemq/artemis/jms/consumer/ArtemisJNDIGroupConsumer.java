package org.activemq.artemis.jms.consumer;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

public class ArtemisJNDIGroupConsumer {

	public static void main(final String[] args) throws Exception {

		final int numMessages = 90;

		Connection connection = null;

		InitialContext initialContext = null;

		try {
			
			// Step 1. Get an initial context for looking up JNDI from the
			// server #1
			initialContext = new InitialContext();

			// Step 2. Look up the JMS resources from JNDI
			Queue queue = (Queue) initialContext.lookup("queue/exampleQueue");
			ConnectionFactory connectionFactory = (ConnectionFactory) initialContext.lookup("ConnectionFactory");

			// Step 3. Create a JMS Connection
			connection = connectionFactory.createConnection();

			// Step 4. Create a *non-transacted* JMS Session with client
			// acknowledgement
			Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

			// Step 5. Start the connection to ensure delivery occurs
			connection.start();

			// Step 6. Create a JMS MessageProducer and a MessageConsumer
			MessageProducer producer = session.createProducer(queue);
			MessageConsumer consumer = session.createConsumer(queue);
			
			// Step 8. Receive and acknowledge a third of the sent messages
			TextMessage message0 = null;
			for (int i = 0; i < numMessages / 3; i++) {
				message0 = (TextMessage) consumer.receive(5000);
				System.out.println("Got message: " + message0.getText());
			}
			message0.acknowledge();

			// Step 9. Receive the rest third of the sent messages but *do not*
			// acknowledge them yet
			for (int i = numMessages / 3; i < numMessages; i++) {
				message0 = (TextMessage) consumer.receive(5000);
				System.out.println("Got message: " + message0.getText());
			}
			message0.acknowledge();
			
			Thread.sleep(1000);
			
		} finally {
			// Step 13. Be sure to close our resources!

			if (connection != null) {
				connection.close();
			}

			if (initialContext != null) {
				initialContext.close();
			}

		}
	}
}
