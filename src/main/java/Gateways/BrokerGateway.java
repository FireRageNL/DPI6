package Gateways;

import forms.LoanBrokerFrame;
import messaging.requestreply.BankTypes;
import messaging.requestreply.QueueTypes;
import messaging.requestreply.Recieve;
import messaging.requestreply.Send;
import model.bank.BankInterestReply;
import model.bank.BankInterestRequest;
import model.loan.LoanReply;
import model.loan.LoanRequest;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.io.Serializable;
import java.util.*;

public class BrokerGateway {

	private LoanBrokerFrame frame;

	private Map<Integer,Integer> sentMessages = new HashMap<Integer,Integer>();
	private Map<Integer,List<BankInterestReply>> recievedMessages = new HashMap<Integer,List<BankInterestReply>>();
	public BrokerGateway(LoanBrokerFrame currentFrame){
		frame = currentFrame;
		RecieveLoanRequest(QueueTypes.LoanRequest.toString());
		RecieveBankReply(QueueTypes.BankReply.toString());
	}

	public void RecieveLoanRequest(String QueueName){
		try {
			Recieve.RecieveObject(QueueName, new MessageListener() {
				public void onMessage(Message message) {
					if(message instanceof ObjectMessage){
						Serializable obj = null;
						try {
							obj = ((ObjectMessage) message).getObject();
						} catch (JMSException e) {
							e.printStackTrace();
						}
						if (obj instanceof LoanRequest ){
							LoanRequest request = (LoanRequest) obj;
							BankInterestRequest bankRequest = new BankInterestRequest(request.getAmount(),request.getTime(),request.getId());
							frame.add(request);
							int messagesSent = ProcessMessagesAndSendToBrokers(bankRequest);
							sentMessages.put(bankRequest.getId(),messagesSent);
							frame.add(request,bankRequest);
						}
					}
				}
			});
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void RecieveBankReply(String QueueName){
		try {
			Recieve.RecieveObject(QueueName, new MessageListener() {
				public void onMessage(Message message) {
					if(message instanceof ObjectMessage){
						Serializable obj = null;
						try {
							obj = ((ObjectMessage) message).getObject();
						} catch (JMSException e) {
							e.printStackTrace();
						}
						if(obj instanceof BankInterestReply){
							BankInterestReply reply = (BankInterestReply) obj;
							ProcessRecievedMessage(reply);
						}
					}
				}
			});
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void ProcessRecievedMessage(BankInterestReply reply){
	if(recievedMessages.containsKey(reply.getId())){
		List<BankInterestReply> replies = recievedMessages.get(reply.getId());
		replies.add(reply);
		if(replies.size() == sentMessages.get(reply.getId())){
			replies.sort(Comparator.comparing(BankInterestReply::getInterest).reversed());
			LoanReply lReply = new LoanReply(reply.getInterest(),reply.getQuoteId(),reply.getId());
			SendMessage(lReply,QueueTypes.LoanReply.toString());
			recievedMessages.remove(reply.getId());
			sentMessages.remove(reply.getId());
			LoanRequest request = frame.getLoanRequest(reply.getId(), reply.getQuoteId());
			frame.add(request,reply);
		}
		else{
			recievedMessages.put(reply.getId(),replies);
		}

	}
	else{
		if(sentMessages.get(reply.getId()) == 1){
			LoanReply lReply = new LoanReply(reply.getInterest(),reply.getQuoteId(),reply.getId());
			SendMessage(lReply,QueueTypes.LoanReply.toString());
			sentMessages.remove(reply.getId());
			LoanRequest request = frame.getLoanRequest(reply.getId(), reply.getQuoteId());
			frame.add(request,reply);
		}
		else{
			List<BankInterestReply> replies = new ArrayList<>();
			replies.add(reply);
			recievedMessages.put(reply.getId(),replies);
		}
	}
	}

	public int ProcessMessagesAndSendToBrokers(BankInterestRequest request){
		int repliesSent = 0;
		if(request.getAmount() <= 100000 && request.getTime() <= 10){
			repliesSent++;
			SendMessage(request,BankTypes.ING.toString());
		}
		if(request.getAmount() >= 200000 && request.getAmount() <= 300000 && request.getTime() <= 20){
			repliesSent++;
			SendMessage(request,BankTypes.ABN.toString());
		}
		if(request.getAmount() <= 250000 && request.getTime() <= 15){
			repliesSent++;
			SendMessage(request,BankTypes.RABO.toString());
		}
		return repliesSent;
	}

	public void SendMessage(Serializable obj, String QueueName){
		try {
			Send.sendObject(obj,QueueName);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
