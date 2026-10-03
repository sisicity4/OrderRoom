import { useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';

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

const statusLabel: Record<string, string> = {
  proposed: '提案中',
  accepted: '採用',
  rejected: '見送り',
};

function HostPage() {
  const { roomId } = useParams<{ roomId: string }>
    ();
  const [searchParams] = useSearchParams();
  const hostKey = searchParams.get('key');
  const [list, setList] = useState<Proposal[]>([]);
  const [error, setError] = useState('');

  const fetchItems = async () => {
    try {
      const res = await fetch(`/api/rooms/${roomId}/items`);
      if (!res.ok) {
        setError("提案の取得に失敗しました");
        return;
      }
      const data = await res.json();
      setList(data);
    } catch {
      setError("通信に失敗しました");
    }
  }

  useEffect(() => {
    fetchItems();
  }, [roomId]);

  if (!hostKey) {
    return <p>管理用URLが正しくありません</p>;
  }
  return (
    <div className='bg-[#f0e5cc] min-h-screen flex flex-col gap-4 p-8'>
      <h1 className='text-2xl font-bold'>ホスト管理</h1>
      {error && <p className='text-red-700'>{error}</p>}
      <ul>
        {list.map((item) => (
          <li className='border border-dashed p-3 flex flex-col gap-1' key={item.id}>
            <p>{item.name} {item.price}円 {item.quantity}個</p>
            <p className='text-sm text-[#7A6B57]'>
              提案者: {item.participantName} / {statusLabel[item.status] ?? item.status} / {item.purchased ? '購入済' : '未購入'}
            </p>
          </li>
        ))}
      </ul>
    </div>
  );
}
export default HostPage;