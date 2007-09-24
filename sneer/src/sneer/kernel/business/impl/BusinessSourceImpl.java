package sneer.kernel.business.impl;

import static wheel.i18n.Language.translate;

import java.awt.Font;

import javax.swing.JLabel;

import sneer.kernel.business.Business;
import sneer.kernel.business.BusinessSource;
import sneer.kernel.business.contacts.ContactAttributes;
import sneer.kernel.business.contacts.ContactAttributesSource;
import sneer.kernel.business.contacts.ContactId;
import sneer.kernel.business.contacts.ContactInfo;
import sneer.kernel.business.contacts.ContactPublicKeyInfo;
import sneer.kernel.business.contacts.impl.ContactAttributesSourceImpl;
import sneer.kernel.business.contacts.impl.ContactPublicKeyUpdater;
import wheel.graphics.JpgImage;
import wheel.io.network.PortNumberSource;
import wheel.lang.Consumer;
import wheel.lang.Counter;
import wheel.lang.Omnivore;
import wheel.lang.Pair;
import wheel.lang.exceptions.IllegalParameter;
import wheel.reactive.Signal;
import wheel.reactive.Source;
import wheel.reactive.impl.SourceImpl;
import wheel.reactive.lists.ListSignal;
import wheel.reactive.lists.ListSource;
import wheel.reactive.lists.impl.ListSourceImpl;


public class BusinessSourceImpl implements BusinessSource  { //Refactor: Create a separate class for BusinessImpl.


	private final class MyOutput implements Business {

		@Override
		public ListSignal<ContactAttributes> contactAttributes() {
			return _contacts.output();
		}

		@Override
		public Signal<String> ownName() {
			return _ownName.output();
		}
		
		@Override
		public Signal<String> language() {
			return _language.output();
		}
		
		@Override
		public Signal<Font> font() {
			return _font.output();
		}

		@Override
		public Signal<Integer> sneerPort() {
			return _sneerPortNumber.output();
		}

		@Override
		public Signal<String> publicKey() {
			return _publicKey.output();
		}
		
		@Override
		public Signal<String> thoughtOfTheDay() {
			return _thoughtOfTheDay.output();
		}
		
		@Override
		public Signal<JpgImage> picture() {
			return _picture.output();
		}
		
		@Override
		public Signal<String> profile() {
			return _profile.output();
		}

	}

	private Source<String> _ownName = new SourceImpl<String>("");
	private Source<String> _language = new SourceImpl<String>("");
	private Source<Font> _font = new SourceImpl<Font>(new JLabel().getFont());
	private final Source<String> _publicKey = new SourceImpl<String>("");
	
	private final Source<String> _thoughtOfTheDay = new SourceImpl<String>("");
	private final Source<JpgImage> _picture = new SourceImpl<JpgImage>(new JpgImage());
	private final Source<String> _profile = new SourceImpl<String>("");

	private final PortNumberSource _sneerPortNumber = new PortNumberSource(0);

	private final ListSource<ContactAttributesSource> _contactSources = new ListSourceImpl<ContactAttributesSource>();
	private final ListSource<ContactAttributes> _contacts = new ListSourceImpl<ContactAttributes>(); 	//Refactor: use a reactive "ListCollector" instead of keeping this redundant list.
	private final Counter _contactIdSource = new Counter();
	
	private final Business _output = new MyOutput();



	@Override
	public Omnivore<String> ownNameSetter() {
		return  _ownName.setter();
	}
	
	@Override
	public Omnivore<String> languageSetter() {
		return  _language.setter();
	}
	
	@Override
	public Omnivore<Font> fontSetter() {
		return  _font.setter();
	}
	
	@Override
	public Consumer<Integer> sneerPortSetter() {
		return _sneerPortNumber.setter();
	}
	
	@Override
	public Omnivore<String> thoughtOfTheDaySetter() {
		return  _thoughtOfTheDay.setter();
	}
	
	@Override
	public Omnivore<JpgImage> pictureSetter() {
		return  _picture.setter();
	}
	
	@Override
	public Omnivore<String> profileSetter() {
		return  _profile.setter();
	}

	@Override
	public Consumer<ContactInfo> contactAdder() {
		return new Consumer<ContactInfo>() { @Override public void consume(ContactInfo info) throws IllegalParameter {
			checkDuplicateNick(info._nick);

			ContactAttributesSource contact = new ContactAttributesSourceImpl(info._nick, info._host, info._port, info._publicKey, _contactIdSource.next(),info._thoughtOfTheDay,info._picture,info._profile);
			_contactSources.add(contact);
			_contacts.add(contact.output());
		}};
	}

	@Override
	public Omnivore<ContactPublicKeyInfo> contactPublicKeyUpdater() {
		return new ContactPublicKeyUpdater(_contactSources);
	}


	@Override
	public Business output() {
		return _output;
	}

	@Override
	@Deprecated
	public Omnivore<sneer.kernel.business.contacts.OnlineEvent> contactOnlineSetter() {
		return new Omnivore<sneer.kernel.business.contacts.OnlineEvent>() { @Override public void consume(sneer.kernel.business.contacts.OnlineEvent ignored) {}};
	}
	
	private ContactAttributesSource findContactSource(String nick) {
		for (ContactAttributesSource candidate:_contactSources.output()) { // Optimize
			if (candidate.output().nick().currentValue().equals(nick))
				return candidate;
		}
		return null;
	}

	public Omnivore<String> publicKeySetter() {
		return _publicKey.setter();
	}

	public Omnivore<ContactId> contactRemover() {
		return new Omnivore<ContactId>() { @Override public void consume(ContactId contactId) {
			ContactAttributesSource contactSource = findContactSource(contactId);
			_contactSources.remove(contactSource);
			_contacts.remove(contactSource.output());
		}};
	}

	private ContactAttributesSource findContactSource(ContactId contactId) {
		for (ContactAttributesSource candidate : _contactSources.output())
			if (candidate.output().id().equals(contactId))
				return candidate;
		
		throw new IllegalArgumentException("contactId not found");
	}

	@Override
	public Consumer<Pair<ContactId, String>> contactNickChanger() {
		return new Consumer<Pair<ContactId,String>>() { @Override public void consume(Pair<ContactId, String> nickChange) throws IllegalParameter {
			ContactId contactId = nickChange._a;
			String newNick = nickChange._b;
			checkDuplicateNick(newNick);
			findContactSource(contactId).nickSetter().consume(newNick);	
		}};

	}

	private void checkDuplicateNick(String newNick) throws IllegalParameter {
		if (findContactSource(newNick) != null)
			throw new IllegalParameter(translate("There already is a contact with nickname: %1$s", newNick));
	};

}
