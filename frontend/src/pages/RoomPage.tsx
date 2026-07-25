import { useState } from 'react'
type Proposal={
    text: string;
    quantity: number;
    price: number;
  };
function RoomPage(){
  const [name,setName]=useState('');
  const [list,setList]=useState<Proposal[]>([]);
  const [price,setPrice]=useState('');
  const [quantity,setQuantity]=useState('');
  const [error,setError]=useState('');
  
  
  return(
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
       type="text"/>
      </div>
      
      
      <hr className='border-t-5' />
     
      
      <p className='border w-fit px-2'>提案</p>
      <ul>
        {list.map((item, index) => (
          <li className='border border-dashed p-3'
           key={index}>{item.text+" "}{item.price+"円 "}{item.quantity+"個"}</li>
          ))}
      </ul>
    <div className='flex '>
      <input 
      className='border outline-none border-dashed'
      type='text'
      value={name}
      placeholder='注文を追加'
      onChange={(e)=> setName(e.target.value)} />

      <input 
            className='border outline-none border-dashed'
            type='text'
            value={price}
            placeholder='値段を追加'
            onChange={(e)=> setPrice(e.target.value)} />

      <input 
      className='border outline-none border-dashed'
      type='text'
      value={quantity}
      placeholder='個数を追加'
      onChange={(e)=> setQuantity(e.target.value)} />
      
    </div>

    {error && <p className='text-red-700'>{error}</p>}
      
      <button onClick={()=>{
        if (name === '' || price === '' || quantity === '') {
      setError("入力欄をすべて入れてください。");
      return;
        }
        setList([...list,{text:name,price:Number(price),quantity:Number(quantity)}]);
        setName('');
        setPrice('');
        setQuantity('');
        setError('');
      }}>提案の追加</button>
    </div>

  );
  
}
export default RoomPage;