package org.activemq.artemis.jms.producer;

import java.util.Properties;

import javax.jms.ConnectionFactory;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiTemplate;

public class ArtemisCamelPoolingProducer {

	public static void main(String argsp[]) throws Exception {

		CamelContext context = new DefaultCamelContext();

		Properties props = new Properties();
		JndiTemplate jndiTemplate = new JndiTemplate();
		props.setProperty("java.naming.factory.initial",
				"org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
		props.setProperty("connectionFactory.ConnectionFactory",
				"(udp://231.7.7.7:9876)?ha=true&retryInterval=1000&retryIntervalMultiplier=1.0&reconnectAttempts=-1");
		props.setProperty("queue.request.queue", "request.queue");
		props.setProperty("queue.response.queue", "response.queue");
		jndiTemplate.setEnvironment(props);

		JndiDestinationResolver jndiDestinationResolver = new JndiDestinationResolver();
		jndiDestinationResolver.setJndiTemplate(jndiTemplate);

		ConnectionFactory connectionFactory = (ConnectionFactory) jndiTemplate.lookup("ConnectionFactory");
		
		CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
		cachingConnectionFactory.setReconnectOnException(true);
		cachingConnectionFactory.setSessionCacheSize(20);
		cachingConnectionFactory.setTargetConnectionFactory(connectionFactory);

		JmsConfiguration jmsConfig = new JmsConfiguration();
		jmsConfig.setConnectionFactory(cachingConnectionFactory);
		jmsConfig.setConcurrentConsumers(5);

		JmsComponent artemis = new JmsComponent();
		artemis.setConfiguration(jmsConfig);
		artemis.setDestinationResolver(jndiDestinationResolver);

		context.addComponent("artemis-jms", artemis);

		context.addRoutes(new RouteBuilder() {
			public void configure() {

				// @formatter:off

				from("direct:hello").routeId("artemis.client.hello").log("Request: ${body}")
						.to("artemis-jms:queue:request.queue");

				from("artemis-jms:queue:response.queue").routeId("artemis.client.response")
						.log("This is the response: ${body}");

				// @formatter:on

			}
		});

		ProducerTemplate template = context.createProducerTemplate();

		context.start();

		for (int i = 0; i < 60000; i++) {
			template.sendBody("direct:hello", "Payload Message: " + i);
			Thread.sleep(1000);
		}

		Thread.sleep(600000);

		context.stop();

	}

}
