package messaging.requestreply;

public enum BankTypes {
	ABN{
		public String ToString(){return "abn";}
	},
	RABO{
		public String ToString(){return"rabo";}
	},
	ING{
		public String ToString(){return"ing";}
	}
}
