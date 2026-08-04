import { useState } from 'react'
type Proposal = {
  text: string;
  quantity: number;
  price: number;
  memo: string;
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


  return (
    <div className='bg-[#f0e5cc] min-h-screen flex flex-col gap-4 p-8'>
      <p className='tracking-[0.5em] text-[#7A6B57]'>おしながき</p>
      <p className='tracking-[1.0em] text-[#7A6B57]'>ルーム名</p>
      <h1 className='text-4xl font-bold'>ルーム名</h1>
      <div className='flex'>
        <p className='border w-fit px-2 '>ID.</p>
        <input
          className='border-b-2 border-neutral-700 ml-1.5 outline-none'
          type="text"
        />

        <p className='border px-2 w-fit ml-auto'>予算</p>
        <input
          className='border-b-2 border-neutral-700 ml-1.5 outline-none'
          type="text" />
      </div>


      <hr className='border-t-5' />



      <div className='flex'>
        <p className='border w-fit px-2'>提案</p>
        <p className='border ml-auto px-2'>合計</p>
        <p className='border-b ml-2 mr-35 tracking-[0.2em]'>{total}円</p>
      </div>
      <ul>
        {list.map((item, index) => (
          <li className='border border-dashed p-3 flex'
            key={index}
            onClick={() => {
              setSelected(index);
              setMemoText(item.memo);
            }}>{item.text + " "}{item.price + "円 "}{item.quantity + "個"}
            <button
              className='border px-2 ml-auto cursor-pointer'
              onClick={(e) => {
                e.stopPropagation();
                setList(list.filter((_item, i) => i !== index));
              }} >削除
            </button>
          </li>
        ))}

      </ul>


      <div className='flex '>
        <input
          className='border outline-none border-dashed'
          type='text'
          value={name}
          placeholder='注文を追加'
          onChange={(e) => setName(e.target.value)} />

        <input
          className='border outline-none border-dashed'
          type='text'
          value={price}
          placeholder='値段を追加'
          onChange={(e) => setPrice(e.target.value)} />

        <input
          className='border outline-none border-dashed'
          type='text'
          value={quantity}
          placeholder='個数を追加'
          onChange={(e) => setQuantity(e.target.value)} />

      </div>

      {error && <p className='text-red-700'>{error}</p>}

      <button onClick={() => {
        if (name === '' || price === '' || quantity === '') {
          setError("入力欄をすべて入れてください。");
          return;
        }
        if (isNaN(Number(price)) || isNaN(Number(quantity))) {
          setError("値段と個数は数字で入れてください。");
          return;
        }
        setList([...list, { text: name, price: Number(price), quantity: Number(quantity), memo: '' }]);
        setName('');
        setPrice('');
        setQuantity('');
        setError('');
      }}>提案の追加</button>

      {selected !== null && (
        <div className='fixed inset-0 bg-black/40 flex items-end'
          onClick={() => setSelected(null)}>
          <div className='bg-[#f0e5cc] w-full rounded-t-2xl p-6 flex flex-col gap-3'
            onClick={(e) => e.stopPropagation()}>
            <p className='border w-fit px-2'>メモ</p>
            <h2 className='text-xl font-bold'>{list[selected].text}</h2>
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