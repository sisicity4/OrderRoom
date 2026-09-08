import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom';
type Proposal = {
  id: string;
  participantName: string;
  name: string;
  price: number;
  quantity: number;
  memo: string;
  status: string;
  purchased: boolean;
};
function RoomPage() {
  const [name, setName] = useState('');
  const [list, setList] = useState<Proposal[]>([]);
  const [price, setPrice] = useState('');
  const [quantity, setQuantity] = useState('');
  const [error, setError] = useState('');
  const [selected, setSelected] = useState<number | null>(null);
  const [memoText, setMemoText] = useState('')

  const total = list.reduce((acc, item) => acc + item.price * item.quantity, 0);
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const stored = localStorage.getItem(`participant:${roomId}`);
  const token: string | null = stored ? JSON.parse(stored).token : null;

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

  useEffect(() => {
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
      <h1 className='text-[clamp(1.75rem,7vw,2.5rem)] font-bold'>ルーム名</h1>
      <div className='flex'>
        <p className='border w-fit px-2 shrink-0'>ID.</p>
        <p
          className='border-b-2 border-neutral-700 ml-1.5 outline-none w-full min-w-0'
        >{roomId}</p>
      </div>

      <div className='flex'>
        <p className='border px-2 w-fit shrink-0'>予算</p>
        <input
          className='border-b-2 border-neutral-700 ml-1.5 outline-none w-full min-w-0'
          type="text" />
      </div>


      <hr className='border-t-5' />



      <div className='flex'>
        <p className='border w-fit px-2 shrink-0'>提案</p>
        <p className='border ml-auto px-2 shrink-0'>合計</p>
        <p className='border-b ml-2 tracking-[0.2em] shrink-0'>{total}円</p>
      </div>
      <ul>
        {list.map((item, index) => (
          <li className='border border-dashed p-3 flex'
            key={item.id}
            onClick={() => {
              setSelected(index);
              setMemoText(item.memo);
            }}>{item.name + " "}{item.price + "円 "}{item.quantity + "個"}
            <button
              className='border px-2 ml-auto shrink-0 cursor-pointer'
              onClick={(e) => {
                e.stopPropagation();
                setList(list.filter((_item, i) => i !== index));
              }} >削除
            </button>
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
        if (name === '' || price === '' || quantity === '') {
          setError("入力欄をすべて入れてください。");
          return;
        }
        if (isNaN(Number(price)) || isNaN(Number(quantity))) {
          setError("値段と個数は数字で入れてください。");
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
              name,
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
          onClick={() => setSelected(null)}>
          <div className='bg-[#f0e5cc] w-full rounded-t-2xl p-6 flex flex-col gap-3'
            onClick={(e) => e.stopPropagation()}>
            <p className='border w-fit px-2'>メモ</p>
            <h2 className='text-xl font-bold'>{list[selected].name}</h2>
            <textarea
              className='border border-dashed outline-none p-2 h-32'
              value={memoText}
              placeholder='メモを記入'
              onChange={(e) => setMemoText(e.target.value)} />
            <div className='flex gap-4'>
              <button onClick={() => {
                setList(list.map((item, i) => i === selected ? { ...item, memo: memoText } : item));
                setSelected(null);
              }}>保存</button>
              <button onClick={() => setSelected(null)}>閉じる</button>
            </div>
          </div>
        </div>
      )}
    </div>

  );

}
export default RoomPage;