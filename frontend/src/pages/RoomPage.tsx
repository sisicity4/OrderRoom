import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom';
type Proposal = {
  id: string;
  participantId: string;
  participantName: string;
  name: string;
  price: number;
  quantity: number;
  memo: string;
  status: string;
  purchased: boolean;
};

const statusLabel: Record<string, string> = {
  proposed: '提案中',
  accepted: '採用',
  rejected: '見送り',
};

const statusColor: Record<string, string> = {
  proposed: 'text-[#7A6B57] border-[#7A6B57]',
  accepted: 'text-red-700 border-red-700',
  rejected: 'text-neutral-500 border-neutral-500',
};

const validateItem = (name: string, price: string, quantity: string) => {
  if (name.trim() === '' || price.trim() === '' || quantity.trim() === '') {
    return "入力欄をすべて入れてください。";
  }
  if (!Number.isInteger(Number(price)) || !Number.isInteger(Number(quantity))) {
    return "値段と個数は整数で入れてください。";
  }
  return '';
};

function RoomPage() {
  const [name, setName] = useState('');
  const [list, setList] = useState<Proposal[]>([]);
  const [price, setPrice] = useState('');
  const [quantity, setQuantity] = useState('');
  const [error, setError] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [memoText, setMemoText] = useState('')
  const [editName, setEditName] = useState('');
  const [editPrice, setEditPrice] = useState('');
  const [editQuantity, setEditQuantity] = useState('');
  const [sheetError, setSheetError] = useState('');
  const [saving, setSaving] = useState(false);
  const [title, setTitle] = useState('');
  const [budgetAmount, setBudgetAmount] = useState<number | null>(null);

  const total = list.reduce((acc, item) => acc + item.price * item.quantity, 0);
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const stored = localStorage.getItem(`participant:${roomId}`);
  const token: string | null = stored ? JSON.parse(stored).token : null;
  const participantId: string | null = stored ? JSON.parse(stored).id : null;
  const selected = list.find((item) => item.id === selectedId) ?? null;

  const fetchRoom = async () => {
    try {
      const res = await fetch(`/api/rooms/${roomId}`);
      if (!res.ok) {
        setError("ルーム情報の取得に失敗しました。");
        return;
      }
      const data = await res.json();
      setTitle(data.title);
      setBudgetAmount(data.budgetAmount);
    } catch {
      setError("通信に失敗しました。");
    }
  };

  const fetchItems = async () => {
    try {
      const res = await fetch(`/api/rooms/${roomId}/items`);
      if (!res.ok) {
        setError("提案の取得に失敗しました。");
        return;
      }
      const data = await res.json();
      setList(data);
    } catch {
      setError("通信に失敗しました。");
    }
  };

  const deleteItem = async (itemId: string) => {
    try {
      const res = await fetch(`/api/rooms/${roomId}/items/${itemId}`, {
        method: 'DELETE',
        headers: {
          'X-Participant-Token': token ?? '',
        },
      });
      if (!res.ok) {
        setError("提案の削除に失敗しました。");
        return;
      }
      await fetchItems();
      setError('');
    } catch {
      setError("通信に失敗しました。");
    }
  };

  const openSheet = (item: Proposal) => {
    setSelectedId(item.id);
    setEditName(item.name);
    setEditPrice(String(item.price));
    setEditQuantity(String(item.quantity));
    setMemoText(item.memo ?? '');
    setSheetError('');
  };

  const saveItem = async (item: Proposal) => {
    if (saving) return;
    const message = validateItem(editName, editPrice, editQuantity);
    if (message !== '') {
      setSheetError(message);
      return;
    }
    const newName = editName.trim();
    const newPrice = Number(editPrice);
    const newQuantity = Number(editQuantity);
    if (newName === item.name && newPrice === item.price && newQuantity === item.quantity && memoText === (item.memo ?? '')) {
      setSelectedId(null);
      return;
    }
    setSaving(true);
    try {
      const res = await fetch(`/api/rooms/${roomId}/items/${item.id}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'X-Participant-Token': token ?? '',
        },
        body: JSON.stringify({
          name: newName,
          price: newPrice,
          quantity: newQuantity,
          memo: memoText,
        }),
      });
      if (!res.ok) {
        setSheetError("提案の保存に失敗しました。");
        return;
      }
      await fetchItems();
      setSelectedId(null);
    } catch {
      setSheetError("通信に失敗しました。");
    } finally {
      setSaving(false);
    }
  };

  useEffect(() => {
    fetchRoom();
    fetchItems();
  }, [roomId]);

  if (!token) {
    return (
      <div className='bg-[#f0e5cc] min-h-screen flex flex-col gap-4 p-8'>
        <p>このルームにまだ参加していません。</p>
        <button className='border px-4 py-2 w-fit' onClick={() => navigate('/join')}>
          参加画面へ
        </button>
      </div>
    );
  }


  return (
    <div className='bg-[#f0e5cc] min-h-screen flex flex-col gap-4 p-8'>
      <p className='tracking-[0.5em] text-[#7A6B57]'>おしながき</p>
      <p className='tracking-[1.0em] text-[#7A6B57]'>ルーム名</p>
      <h1 className='text-[clamp(1.75rem,7vw,2.5rem)] font-bold break-all'>{title}</h1>
      <div className='flex'>
        <p className='border w-fit px-2 shrink-0'>ID.</p>
        <p
          className='border-b-2 border-neutral-700 ml-1.5 outline-none w-full min-w-0 break-all'
        >{roomId}</p>
      </div>

      <div className='flex'>
        <p className='border px-2 w-fit shrink-0'>予算</p>
        <p className='border-b-2 border-neutral-700 ml-1.5 w-full min-w-0'>
          {budgetAmount === null ? '未設定' : `${budgetAmount}円`}
        </p>
      </div>


      <hr className='border-t-5' />



      <div className='flex'>
        <p className='border w-fit px-2 shrink-0'>提案</p>
        <p className='border ml-auto px-2 shrink-0'>合計</p>
        <p className='border-b ml-2 tracking-[0.2em] shrink-0'>{total}円</p>
      </div>
      <ul className='flex flex-col gap-2'>
        {list.map((item) => (
          <li className={`border border-dashed p-3 flex gap-2 cursor-pointer ${item.status === 'rejected' ? 'opacity-60' : ''}`}
            key={item.id}
            onClick={() => openSheet(item)}>
            <div className='flex flex-col min-w-0'>
              <p className='break-all'>{item.name + " "}{item.price + "円 "}{item.quantity + "個"}</p>
              <p className='text-sm text-[#7A6B57] break-all'>提案者: {item.participantName}</p>
              <div className='flex flex-wrap gap-1 mt-1'>
                <p className={`border px-2 text-sm w-fit ${statusColor[item.status] ?? ''}`}>
                  {statusLabel[item.status] ?? item.status}
                </p>
                {item.purchased && (
                  <p className='border border-red-700 text-red-700 px-2 text-sm w-fit'>購入済</p>
                )}
                {item.memo && (
                  <p className='border border-dashed px-2 text-sm w-fit text-[#7A6B57]'>メモあり</p>
                )}
              </div>
            </div>
            {item.participantId === participantId && (
              <button
                className='border px-2 ml-auto shrink-0 self-start cursor-pointer'
                onClick={(e) => {
                  e.stopPropagation();
                  deleteItem(item.id);
                }} >削除
              </button>
            )}
          </li>
        ))}

      </ul>


      <div className='flex flex-col gap-2 sm:flex-row'>
        <input
          className='border outline-none border-dashed px-2 min-w-0'
          type='text'
          value={name}
          placeholder='注文を追加'
          onChange={(e) => setName(e.target.value)} />

        <input
          className='border outline-none border-dashed px-2 min-w-0'
          type='text'
          value={price}
          placeholder='値段を追加'
          onChange={(e) => setPrice(e.target.value)} />

        <input
          className='border outline-none border-dashed px-2 min-w-0'
          type='text'
          value={quantity}
          placeholder='個数を追加'
          onChange={(e) => setQuantity(e.target.value)} />

      </div>

      {error && <p className='text-red-700'>{error}</p>}

      <button onClick={async () => {
        const message = validateItem(name, price, quantity);
        if (message !== '') {
          setError(message);
          return;
        }
        try {
          const res = await fetch(`/api/rooms/${roomId}/items`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'X-Participant-Token': token,
            },
            body: JSON.stringify({
              name: name.trim(),
              price: Number(price),
              quantity: Number(quantity),
              memo: '',
            }),
          });
          if (!res.ok) {
            setError("提案の追加に失敗しました。");
            return;
          }
          await fetchItems();
          setName('');
          setPrice('');
          setQuantity('');
          setError('');
        } catch {
          setError("通信に失敗しました。");
        }
      }}>提案の追加</button>

      {selected !== null && (
        <div className='fixed inset-0 bg-black/40 flex items-end'
          onClick={() => setSelectedId(null)}>
          <div className='bg-[#f0e5cc] w-full rounded-t-2xl p-6 flex flex-col gap-3 max-h-[90vh] overflow-y-auto'
            onClick={(e) => e.stopPropagation()}>
            <p className='border w-fit px-2'>提案の詳細</p>
            {selected.participantId === participantId ? (
              <>
                <div className='flex'>
                  <p className='border px-2 shrink-0'>品名</p>
                  <input
                    className='border-b-2 border-neutral-700 ml-1.5 outline-none w-full min-w-0 bg-transparent'
                    type='text'
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)} />
                </div>
                <div className='flex'>
                  <p className='border px-2 shrink-0'>値段</p>
                  <input
                    className='border-b-2 border-neutral-700 ml-1.5 outline-none w-full min-w-0 bg-transparent'
                    type='text'
                    inputMode='numeric'
                    value={editPrice}
                    onChange={(e) => setEditPrice(e.target.value)} />
                  <p className='ml-1 shrink-0'>円</p>
                </div>
                <div className='flex'>
                  <p className='border px-2 shrink-0'>個数</p>
                  <input
                    className='border-b-2 border-neutral-700 ml-1.5 outline-none w-full min-w-0 bg-transparent'
                    type='text'
                    inputMode='numeric'
                    value={editQuantity}
                    onChange={(e) => setEditQuantity(e.target.value)} />
                  <p className='ml-1 shrink-0'>個</p>
                </div>
                <p className='border w-fit px-2'>メモ</p>
                <textarea
                  className='border border-dashed outline-none p-2 h-32'
                  value={memoText}
                  placeholder='メモを記入'
                  onChange={(e) => setMemoText(e.target.value)} />
                {selected.status === 'accepted' && (
                  <p className='text-sm text-[#7A6B57]'>
                    採用済みの提案を変更すると「提案中」に戻ります。
                    {selected.purchased && '購入済みと購入情報も解除されます。'}
                  </p>
                )}
                {sheetError && <p className='text-red-700'>{sheetError}</p>}
                <div className='flex gap-4'>
                  <button
                    className='disabled:opacity-40'
                    disabled={saving}
                    onClick={() => saveItem(selected)}>{saving ? '保存中…' : '保存'}</button>
                  <button onClick={() => setSelectedId(null)}>閉じる</button>
                </div>
              </>
            ) : (
              <>
                <h2 className='text-xl font-bold break-all'>{selected.name}</h2>
                <p>{selected.price}円 × {selected.quantity}個</p>
                <p className='border w-fit px-2'>メモ</p>
                <p className='border-y border-dashed p-2 min-h-16 whitespace-pre-wrap break-all'>
                  {selected.memo ? selected.memo : 'メモはありません。'}
                </p>
                <div className='flex gap-4'>
                  <button onClick={() => setSelectedId(null)}>閉じる</button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>

  );

}
export default RoomPage;
