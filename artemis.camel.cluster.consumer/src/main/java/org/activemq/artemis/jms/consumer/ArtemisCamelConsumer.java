package org.activemq.artemis.jms.consumer;

import java.util.Properties;

import javax.jms.ConnectionFactory;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiTemplate;

public final class ArtemisCamelConsumer {

	static int i = 0;

	private ArtemisCamelConsumer() {

	}

	public static void main(String argsp[]) throws Exception {
		
		System.out.println("Starting Consumer");

		CamelContext context = new DefaultCamelContext();

		Properties props = new Properties();
		JndiTemplate jndiTemplate = new JndiTemplate();
		props.setProperty("java.naming.factory.initial",
				"org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
		// props.setProperty("connectionFactory.ConnectionFactory",
		//		"(tcp://localhost:61816,tcp://localhost:61916)?ha=true&retryInterval=1000&retryIntervalMultiplier=1.0&reconnectAttempts=-1");
		props.setProperty("connectionFactory.ConnectionFactory",
				"(tcp://localhost:61616,tcp://localhost:61716)?ha=true&retryInterval=1000&retryIntervalMultiplier=1.0&reconnectAttempts=-1");
		props.setProperty("queue.request.queue", "request.queue");
		props.setProperty("queue.response.queue", "response.queue");
		jndiTemplate.setEnvironment(props);

		JndiDestinationResolver jndiDestinationResolver = new JndiDestinationResolver();
		jndiDestinationResolver.setJndiTemplate(jndiTemplate);

		ConnectionFactory connectionFactory = (ConnectionFactory) jndiTemplate.lookup("ConnectionFactory");

		JmsConfiguration jmsConfig = new JmsConfiguration();
		jmsConfig.setConnectionFactory(connectionFactory);
		jmsConfig.setConcurrentConsumers(5);

		JmsComponent artemis = new JmsComponent();
		artemis.setConfiguration(jmsConfig);
		artemis.setDestinationResolver(jndiDestinationResolver);

		context.addComponent("artemis-jms", artemis);

		context.addRoutes(new RouteBuilder() {
			public void configure() {

				// @formatter:off

				from("artemis-jms:queue:request.queue?replyTo=response.queue&replyToType=Exclusive")
						.routeId("cluster.consumer").log("Request : ${body}").transform().simple("Response : ${body}");

				// @formatter:on

			}
		});

		context.start();
		
		Thread.sleep(600000);

		context.stop();

	}

}
